import axios from "axios"

let globalShowError: ((message: string) => void) | null = null

export const setGlobalToast = (showError: (message: string) => void) => {
    globalShowError = showError
}

export const httpClient = axios.create({
    baseURL: '/api',
    withCredentials: true,
})

httpClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config

        if (error.response?.status === 401) {
            try {
                await axios.post(`/api/auth/refresh`, {}, {withCredentials: true})
                return httpClient(originalRequest)
            } catch (refreshError) {
                if (globalShowError) {
                    globalShowError("Your session has expired. Please log in again.")
                }
                return Promise.reject(refreshError)
            }
        }

        if (globalShowError && error.response) {
            let {detail, status} = error.response.data

            globalShowError(`${status}: ${detail}`)
        }

        return Promise.reject(error)
    },
)
