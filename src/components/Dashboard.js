import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Button, Typography, Dialog, DialogTitle, DialogContent, TextField, DialogActions } from '@mui/material';
import api from '../api';

const Dashboard = () => {
    const navigate = useNavigate();
    const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
    const [loginRole, setLoginRole] = useState(''); // 'ReceptionStaff' or 'MaintenanceTeam'
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');

    const handleLogin = async () => {
        try {
            const response = await api.post('/login', { username, password });
            const { token, roles } = response.data;

            localStorage.setItem('token', token);

            if (roles.includes('ReceptionStaff') && loginRole === 'ReceptionStaff') {
                navigate('/reception');
            } else if (roles.includes('MaintenanceTeam') && loginRole === 'MaintenanceTeam') {
                navigate('/maintenance');
            } else {
                setError('Role mismatch. Please check your login credentials.');
            }

            setIsLoginModalOpen(false);
        } catch (error) {
            console.error('Login failed:', error);
            setError('Invalid credentials. Please try again.');
        }
    };

    return (
        <Box sx={{ textAlign: 'center', padding: 4 }}>
            <Typography variant="h4" sx={{ marginBottom: 4 }}>
                Welcome to the Equipment Management System
            </Typography>
            <Button
                variant="contained"
                color="primary"
                sx={{ marginRight: 2 }}
                onClick={() => {
                    setLoginRole('ReceptionStaff');
                    setIsLoginModalOpen(true);
                }}
            >
                Login as Reception Staff
            </Button>
            <Button
                variant="contained"
                color="secondary"
                onClick={() => {
                    setLoginRole('MaintenanceTeam');
                    setIsLoginModalOpen(true);
                }}
            >
                Login as Maintenance Team
            </Button>

            {/* Login Modal */}
            <Dialog open={isLoginModalOpen} onClose={() => setIsLoginModalOpen(false)}>
                <DialogTitle>Login</DialogTitle>
                <DialogContent>
                    <TextField
                        label="Username"
                        fullWidth
                        margin="normal"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                    />
                    <TextField
                        label="Password"
                        type="password"
                        fullWidth
                        margin="normal"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                    />
                    {error && <Typography color="error">{error}</Typography>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setIsLoginModalOpen(false)} color="secondary">
                        Cancel
                    </Button>
                    <Button onClick={handleLogin} color="primary">
                        Login
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default Dashboard;
