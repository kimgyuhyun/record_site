#!/bin/bash
# 최소권한 DB 계정 자동 생성 (PLATFORM 4절).
#
# 왜 스크립트인가:
#   loldb_app / loldb_migrate 는 지금까지 서버에서 손으로 만들어졌고, 저장소 어디에도
#   생성 코드가 없었다. 그래서 (1) mysql 볼륨을 새로 만들면 앱이 접속하지 못해 health 가
#   실패하고 배포가 롤백 루프에 빠지며, (2) 두 계정에 실제로 어떤 권한이 붙어 있는지
#   아무도 검증할 수 없었다. 오라클 인스턴스가 사라지거나 백업에서 되살리는 시나리오는
#   이 프로젝트에서 현실적인 경로라 코드로 남긴다.
#
# 언제 실행되나:
#   /docker-entrypoint-initdb.d 에 마운트돼 있어 **데이터 볼륨을 새로 만들 때 딱 한 번**
#   자동 실행된다. 이미 데이터가 있는 볼륨에서는 실행되지 않는다 —
#   즉 지금 돌고 있는 운영 DB 는 이 파일을 넣어도 아무 변화가 없다.
#
# 살아 있는 DB 에 소급 적용하려면 아래 GRANT 문을 root 로 직접 실행한다:
#   docker exec -it lol-mysql mysql -uroot -p
#
# .sql 이 아니라 .sh 인 이유: 계정명·비밀번호를 .env 에서 받아야 하는데 .sql 파일은
# 환경변수를 읽지 못한다. 엔트리포인트는 initdb.d 안의 .sh 도 실행해 준다.
#
# ⚠️ 전체를 함수로 감싸고 최상위에서 exit / set -eu 를 쓰지 않는 이유:
#   mysql 엔트리포인트는 실행 비트가 없는 .sh 를 실행하지 않고 `. "$f"` 로 **source** 한다.
#   그 경우 최상위 `exit 0` 은 이 스크립트가 아니라 엔트리포인트를 통째로 끝내버려
#   초기화가 중간에 멈춘다(테이블도 계정도 없는 DB 가 남는다). `set -eu` 도 마찬가지로
#   엔트리포인트 셸에 그대로 남아 이후 동작을 바꾼다.
#   실행 비트는 .gitattributes 와 별개로 git 에 기록해 두었지만, 비트가 빠져도 안전하도록
#   소스되든 실행되든 같게 동작하게 만든다. 실패했을 때만 exit 1 로 초기화를 중단시킨다
#   — 권한이 잘못 붙은 채로 뜨는 것보다 크게 실패하는 편이 낫다.

_lp_main() {
  local app_user="${DB_APP_USER:-}"
  local app_pw_raw="${DB_APP_PASSWORD:-}"
  local mig_user="${DB_MIGRATE_USER:-}"
  local mig_pw_raw="${DB_MIGRATE_PASSWORD:-}"
  local db="${MYSQL_DATABASE:-loldb}"
  local u app_pw mig_pw

  # 넷 중 하나라도 비면 계정을 만들지 않는다. 비밀번호 없는 계정이 생기는 것보다 낫고,
  # 이 경우 앱은 application.yaml 의 폴백대로 root 로 붙는다(개발 환경의 기존 동작).
  if [ -z "$app_user" ] || [ -z "$app_pw_raw" ] || [ -z "$mig_user" ] || [ -z "$mig_pw_raw" ]; then
    echo "[least-privilege] DB_APP_*/DB_MIGRATE_* 미설정 — 계정 생성을 건너뜁니다(개발 환경)."
    return 0
  fi

  # 계정명은 식별자라 자리표시자로 넘길 수 없어 문자열로 조립된다. 형식을 확인해 두면
  # 오타가 이상한 SQL 로 번지지 않는다.
  for u in "$app_user" "$mig_user"; do
    case "$u" in
      *[!A-Za-z0-9_]*|"")
        echo "[least-privilege] 계정명이 올바르지 않습니다: $u" >&2
        return 1 ;;
    esac
  done

  # 비밀번호는 값 자리에 들어가므로 작은따옴표와 역슬래시만 이스케이프한다.
  app_pw=$(printf '%s' "$app_pw_raw" | sed "s/\\\\/\\\\\\\\/g; s/'/''/g")
  mig_pw=$(printf '%s' "$mig_pw_raw" | sed "s/\\\\/\\\\\\\\/g; s/'/''/g")

  echo "[least-privilege] 계정 생성: ${app_user}(DML) / ${mig_user}(DDL) on ${db}"

  # 비밀번호는 argv 가 아니라 MYSQL_PWD 로 넘긴다(컨테이너 안 ps 노출 회피).
  MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket -uroot <<SQL
-- 런타임 계정: DML 만. 스키마를 바꿀 수 없다.
-- 데이터베이스 단위로 주므로 앞으로 마이그레이션이 만들 테이블에도 자동으로 적용된다
-- (PLATFORM 4절의 "앞으로 만들 테이블에도 권한이 붙을 것"을 MySQL 에서는 이렇게 만족한다).
CREATE USER IF NOT EXISTS '${app_user}'@'%' IDENTIFIED BY '${app_pw}';
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${db}\`.* TO '${app_user}'@'%';

-- 마이그레이션 계정: 이 데이터베이스에 한해 DDL 가능. 슈퍼유저가 아니다.
-- DML 도 필요하다 — flyway_schema_history 에 이력을 기록해야 하기 때문.
-- 권한을 이 데이터베이스로 한정해, 자격증명이 새도 다른 스키마나 서버 파일에는 닿지 못한다.
CREATE USER IF NOT EXISTS '${mig_user}'@'%' IDENTIFIED BY '${mig_pw}';
GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, ALTER, DROP, INDEX, REFERENCES, CREATE TEMPORARY TABLES
   ON \`${db}\`.* TO '${mig_user}'@'%';

FLUSH PRIVILEGES;
SQL
  local rc=$?
  if [ "$rc" -ne 0 ]; then
    echo "[least-privilege] GRANT 실행 실패(rc=$rc)" >&2
    return 1
  fi

  echo "[least-privilege] 완료"
  return 0
}

if ! _lp_main; then
  echo "[least-privilege] 초기화를 중단합니다 — 권한이 잘못 붙은 DB 로 기동하지 않습니다." >&2
  exit 1
fi
unset -f _lp_main
