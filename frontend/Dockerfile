# ── 1단계: Vite 빌드 ──
FROM node:20-alpine AS build
WORKDIR /app

# 의존성 레이어 캐싱
COPY package.json package-lock.json ./
RUN npm ci

# 소스 복사 후 정적 번들 빌드 (public/ 의 cdn·static 자산도 dist 로 복사된다)
COPY . .
RUN npm run build

# ── 2단계: nginx 정적 서빙 ──
# 이 컨테이너는 SPA 정적 파일만 서빙한다. /api 프록시는 앞단 edge nginx 가 담당한다.
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
