import React from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { Button, Box } from '@mui/material';
import api from '../api'; // Assuming api is your Axios instance

const Layout = () => {
    const navigate = useNavigate();

    const handleLogout = async () => {
        const token = localStorage.getItem('token');
        if (!token) return;

        try {
            // Make the logout API call
            await api.post('/logout', {}, {
                headers: {
                    Authorization: `Bearer ${token}`
                }
            });

            // On success, clear token and redirect
            localStorage.removeItem('token');
            navigate('/');
        } catch (error) {
            console.error('Logout failed:', error);
            // Optional: Handle errors (e.g., show a message to the user)
        }
    };

    return (
        <Box>
            {/* Add Logout Button */}
            <Box sx={{ textAlign: 'right', padding: 2 }}>
                <Button variant="outlined" color="secondary" onClick={handleLogout}>
                    Logout
                </Button>
            </Box>
            {/* Render child components */}
            <Outlet />
        </Box>
    );
};

export default Layout;
