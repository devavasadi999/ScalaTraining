import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

console.log('Hi');
console.log(API_BASE_URL);// Fallback to localhost if env var is not set

const api = axios.create({
    baseURL: API_BASE_URL,
});

export default api;
