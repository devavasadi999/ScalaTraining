import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Button, Typography } from '@mui/material';
import api from '../api';

const Dashboard = () => {
    const navigate = useNavigate();

    return (
        <Box sx={{ textAlign: 'center', padding: 4 }}>
            <Typography variant="h4" sx={{ marginBottom: 4 }}>
                Welcome to the Equipment Management System
            </Typography>
            <Button
                variant="contained"
                color="primary"
                sx={{ marginRight: 2 }}
                onClick={() => navigate('/reception')}
            >
                Login as Reception Staff
            </Button>
            <Button
                variant="contained"
                color="secondary"
                onClick={() => navigate('/maintenance')}
            >
                Login as Maintenance Team
            </Button>
        </Box>
    );
};

export default Dashboard;
