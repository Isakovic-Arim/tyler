import axios from "axios"

export const httpClient = axios.create({
    baseURL: 'http://localhost:8080',
    withCredentials: true,
})

httpClient.interceptors.response.use(
    response => response,
    async error => {
        const originalRequest = error.config
        if (error.response?.status === 401) {
            try {
                await axios.post('http://localhost:8080/auth/refresh', {}, { withCredentials: true })
                return httpClient(originalRequest)
            } catch (error) {
                return Promise.reject(error)
            }
        }
        return Promise.reject(error)
    }
)
