import React, { useState } from 'react';
import {httpClient} from "~/service";
import {useNavigate} from "react-router";

export default function Component() {
    const navigate = useNavigate();

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [message, setMessage] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        try {
            const response = await httpClient.post('/auth/login', {
                username,
                password,
            })
            if (response.status === 200) {
                setMessage('Login successful!');
                navigate('/');
            } else if (response.status === 401) {
                setMessage('Invalid credentials.');
            } else {
                setMessage('Login failed.');
            }
        } catch (error) {
            console.error('Error during login:', error);
            setMessage('An error occurred.');
        }
    };

    return (
        <>
            <h2>Login</h2>
            <form onSubmit={handleSubmit}>
                <div>
                    <label>
                        Username:
                        <input
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                        />
                    </label>
                </div>
                <div>
                    <label>
                        Password:
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </label>
                </div>
                <button type="submit">Login</button>
            </form>
            {message && <p>{message}</p>}
        </>
    );
}
