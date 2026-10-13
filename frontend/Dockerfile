# ── 1단계: Vite 빌드 ──
# 베이스 이미지는 digest 로 고정한다. 태그가 가리키는 이미지가 바뀌어도 빌드가 조용히 달라지지 않고 Dependabot PR 로 드러난다.
FROM node:25-alpine@sha256:bdf2cca6fe3dabd014ea60163eca3f0f7015fbd5c7ee1b0e9ccb4ced6eb02ef4 AS build
WORKDIR /app

# 의존성 레이어 캐싱
COPY package.json package-lock.json ./
RUN npm ci

# 소스 복사 후 정적 번들 빌드 (public/ 의 cdn·static 자산도 dist 로 복사된다)
COPY . .
RUN npm run build

# ── 2단계: nginx 정적 서빙 ──
# 이 컨테이너는 SPA 정적 파일만 서빙한다. /api 프록시는 앞단 edge nginx 가 담당한다.
FROM nginx:1.31-alpine@sha256:df221db836e1754089190208cee7eeda94f233197056426eda74a43ab1abeac2
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
