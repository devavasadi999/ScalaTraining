import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Box,
    Button,
    Typography,
    TextField,
    Select,
    MenuItem,
    FormControl,
    InputLabel,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle
} from '@mui/material';
import api from '../api';

const HomePage = () => {
    const navigate = useNavigate();
    const [serviceTeams, setServiceTeams] = useState([]);
    const [selectedServiceTeam, setSelectedServiceTeam] = useState('');
    const [isLoginDialogOpen, setIsLoginDialogOpen] = useState(false);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [loginFor, setLoginFor] = useState(''); // 'eventManager' or 'serviceTeam'

    // Fetch service teams on component mount
    useEffect(() => {
        const fetchServiceTeams = async () => {
            try {
                const response = await api.get('/serviceTeams');
                setServiceTeams(response.data);
            } catch (error) {
                console.error('Error fetching service teams:', error);
            }
        };

        fetchServiceTeams();
    }, []);

    // Handle user login
    const handleLogin = async () => {
        try {
            const response = await api.post('/login', { username, password });
            const { token } = response.data;

            // Store token in localStorage
            localStorage.setItem('token', token);

            if (loginFor === 'eventManager') {
                navigate('/event-plans');
            } else if (loginFor === 'serviceTeam' && selectedServiceTeam) {
                navigate(`/service-team-tasks?serviceTeamId=${selectedServiceTeam}`);
            } else {
                console.error('Invalid login role or missing service team selection');
            }

            // Close dialog
            setIsLoginDialogOpen(false);
        } catch (error) {
            console.error('Login failed:', error);
        }
    };

    // Open login dialog for the specified role
    const openLoginDialog = (loginType) => {
        setLoginFor(loginType);
        setIsLoginDialogOpen(true);
    };

    return (
        <Box sx={{ textAlign: 'center', padding: 4 }}>
            <Typography variant="h4" sx={{ marginBottom: 4 }}>
                Welcome to Event Management
            </Typography>
            <Button
                variant="contained"
                color="primary"
                sx={{ marginBottom: 4 }}
                onClick={() => openLoginDialog('eventManager')}
            >
                Login as Event Manager
            </Button>
            <Box sx={{ maxWidth: 400, margin: 'auto', textAlign: 'center' }}>
                <FormControl fullWidth sx={{ marginBottom: 2 }}>
                    <InputLabel id="service-team-select-label">Select Service Team</InputLabel>
                    <Select
                        labelId="service-team-select-label"
                        value={selectedServiceTeam}
                        onChange={(e) => setSelectedServiceTeam(e.target.value)}
                    >
                        {serviceTeams.map((team) => (
                            <MenuItem key={team.id} value={team.id}>
                                {team.name}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
                <Button
                    variant="contained"
                    color="primary"
                    onClick={() => openLoginDialog('serviceTeam')}
                    disabled={!selectedServiceTeam}
                >
                    Login as Service Team
                </Button>
            </Box>

            {/* Login Dialog */}
            <Dialog open={isLoginDialogOpen} onClose={() => setIsLoginDialogOpen(false)}>
                <DialogTitle>Login</DialogTitle>
                <DialogContent>
                    <TextField
                        fullWidth
                        margin="normal"
                        label="Username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                    />
                    <TextField
                        fullWidth
                        margin="normal"
                        label="Password"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setIsLoginDialogOpen(false)} color="secondary">
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

export default HomePage;
