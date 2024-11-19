import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { Box, Button, Typography, Select, MenuItem, FormControl, InputLabel } from '@mui/material';

const HomePage = () => {
    const navigate = useNavigate();
    const [serviceTeams, setServiceTeams] = useState([]);
    const [selectedServiceTeam, setSelectedServiceTeam] = useState('');

    useEffect(() => {
        const fetchServiceTeams = async () => {
            try {
                const response = await axios.get('http://localhost:9000/serviceTeams');
                setServiceTeams(response.data);
            } catch (error) {
                console.error('Error fetching service teams:', error);
            }
        };

        fetchServiceTeams();
    }, []);

    const handleServiceTeamLogin = () => {
        if (selectedServiceTeam) {
            navigate(`/service-team-tasks?serviceTeamId=${selectedServiceTeam}`);
        }
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
                onClick={() => navigate('/event-plans')}
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
                    onClick={handleServiceTeamLogin}
                    disabled={!selectedServiceTeam}
                >
                    Login as Service Team
                </Button>
            </Box>
        </Box>
    );
};

export default HomePage;
