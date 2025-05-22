import React, { useState } from 'react';
import {httpClient} from "~/service";
import {useNavigate} from "react-router";

export default function Component() {
    const navigate = useNavigate()

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [message, setMessage] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        try {
            const response = await httpClient.post('auth/register', {
                username,
                password,
            })
            if (response.status === 200) {
                setMessage('Registration successful!');
                navigate('/login');
            } else if (response.status === 409) {
                setMessage('Username already exists.');
            } else {
                setMessage('Registration failed.');
            }
        } catch (error) {
            console.error('Error:', error);
            setMessage('An error occurred.');
        }
    };

    return (
        <>
            <h2>Register</h2>
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
                <button type="submit">Register</button>
            </form>
            {message && <p>{message}</p>}
        </>
    );
}
