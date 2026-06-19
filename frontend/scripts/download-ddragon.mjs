// Data Dragon 정적 이미지(챔피언/아이템/소환사 스킬/룬 아이콘)를 patch 버전 단위로 받아오는 배치 스크립트.
// 사용법: node scripts/download-ddragon.mjs [version]
//   version을 생략하면 ddragon versions.json의 최신 버전을 사용한다.
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";

const DDRAGON_ROOT = "https://ddragon.leagueoflegends.com";
const CONCURRENCY = 8; // ddragon CDN에 과도한 동시 요청을 피하기 위한 제한

async function getLatestVersion() {
  const res = await fetch(`${DDRAGON_ROOT}/api/versions.json`);
  const versions = await res.json();
  return versions[0];
}

async function fetchJson(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`GET ${url} -> ${res.status}`);
  return res.json();
}

async function downloadFile(url, destPath) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`GET ${url} -> ${res.status}`);
  await mkdir(path.dirname(destPath), { recursive: true });
  const buf = Buffer.from(await res.arrayBuffer());
  await writeFile(destPath, buf);
}

// 동시 다운로드 개수를 CONCURRENCY로 제한해서 처리
async function downloadAll(tasks) {
  let cursor = 0;
  let failed = 0;
  async function worker() {
    while (cursor < tasks.length) {
      const i = cursor++;
      const { url, dest } = tasks[i];
      try {
        await downloadFile(url, dest);
      } catch (e) {
        failed++;
        console.error(`  실패: ${url} (${e.message})`);
      }
    }
  }
  await Promise.all(Array.from({ length: CONCURRENCY }, worker));
  return failed;
}

async function collectChampionTasks(version, baseDir) {
  const { data } = await fetchJson(`${DDRAGON_ROOT}/cdn/${version}/data/ko_KR/champion.json`);
  return Object.values(data).map((champ) => ({
    url: `${DDRAGON_ROOT}/cdn/${version}/img/champion/${champ.image.full}`,
    dest: path.join(baseDir, "img/champion", champ.image.full),
  }));
}

async function collectItemTasks(version, baseDir) {
  const { data } = await fetchJson(`${DDRAGON_ROOT}/cdn/${version}/data/ko_KR/item.json`);
  return Object.values(data).map((item) => ({
    url: `${DDRAGON_ROOT}/cdn/${version}/img/item/${item.image.full}`,
    dest: path.join(baseDir, "img/item", item.image.full),
  }));
}

async function collectSpellTasks(version, baseDir) {
  const { data } = await fetchJson(`${DDRAGON_ROOT}/cdn/${version}/data/ko_KR/summoner.json`);
  return Object.values(data).map((spell) => ({
    url: `${DDRAGON_ROOT}/cdn/${version}/img/spell/${spell.image.full}`,
    dest: path.join(baseDir, "img/spell", spell.image.full),
  }));
}

// 룬 아이콘은 버전 디렉터리가 아니라 /cdn/img/{icon} 경로에서 받아온다 (icon 필드에 전체 상대경로가 들어있음)
async function collectRuneTasks(version, baseDir) {
  const paths = await fetchJson(`${DDRAGON_ROOT}/cdn/${version}/data/ko_KR/runesReforged.json`);
  const tasks = [];
  for (const runePath of paths) {
    tasks.push({
      url: `${DDRAGON_ROOT}/cdn/img/${runePath.icon}`,
      dest: path.join(baseDir, "cdn-img", runePath.icon),
    });
    for (const slot of runePath.slots) {
      for (const rune of slot.runes) {
        tasks.push({
          url: `${DDRAGON_ROOT}/cdn/img/${rune.icon}`,
          dest: path.join(baseDir, "cdn-img", rune.icon),
        });
      }
    }
  }
  return tasks;
}

async function main() {
  const version = process.argv[2] ?? (await getLatestVersion());
  const baseDir = path.resolve(import.meta.dirname, "..", "public", "cdn", version);

  console.log(`Data Dragon ${version} 에셋을 ${baseDir} 에 다운로드합니다.`);

  const [champions, items, spells, runes] = await Promise.all([
    collectChampionTasks(version, baseDir),
    collectItemTasks(version, baseDir),
    collectSpellTasks(version, baseDir),
    collectRuneTasks(version, baseDir),
  ]);

  const groups = [
    ["챔피언", champions],
    ["아이템", items],
    ["소환사 스킬", spells],
    ["룬", runes],
  ];

  let totalFailed = 0;
  for (const [label, tasks] of groups) {
    console.log(`${label} 아이콘 ${tasks.length}개 다운로드 중...`);
    totalFailed += await downloadAll(tasks);
  }

  console.log(`완료. 실패 ${totalFailed}건.`);
  if (totalFailed === 0) {
    console.log(`프론트 코드의 DDRAGON_VERSION, 백엔드 DataDragonService.VERSION 을 "${version}" 으로 갱신하세요.`);
  }
}

main().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
