import React from 'react';
import { Box, Typography, Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import api from '../api';

const ReceptionStaff = () => {
    const navigate = useNavigate();

    return (
        <Box sx={{ textAlign: 'center', padding: 4 }}>
            <Typography variant="h4" sx={{ marginBottom: 4 }}>
                Reception Staff Dashboard
            </Typography>
            <Button
                variant="contained"
                color="primary"
                sx={{ marginRight: 2 }}
                onClick={() => navigate('/employees')}
            >
                View Employees
            </Button>
            <Button
                variant="contained"
                color="secondary"
                onClick={() => navigate('/equipment-types')}
            >
                View Equipment Types
            </Button>
        </Box>
    );
};

export default ReceptionStaff;
