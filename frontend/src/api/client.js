import axios from 'axios';

const apiClient = axios.create({
    withCredentials: false, // 쿠키 안 쓰면 false, 필요하면 true
    headers: {
        'Content-Type': 'application/json',
        },
    });

export default apiClient;

// 프론트엔드에서 백엔드로 HTTP 요청을 보낼 때 공통 설정을 모아둔 파일