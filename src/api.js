import axios from 'axios';

const API_BASE_URL = "http://localhost:9000"; // Replace with your backend URL

const api = axios.create({
    baseURL: API_BASE_URL,
});

export default api;