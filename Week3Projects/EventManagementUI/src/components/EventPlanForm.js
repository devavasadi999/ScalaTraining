import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TextField, Button, Box, Typography, MenuItem } from '@mui/material';
import axios from 'axios';
import api from '../api';

const eventTypes = ['Wedding', 'CorporateEvent', 'Birthday'];

const EventPlanForm = () => {
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        event_type: '',
        date: '',
        expected_guest_count: 0,
    });

    const navigate = useNavigate();

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prevData) => ({
            ...prevData,
            [name]: name === 'expected_guest_count' ? parseInt(value, 10) : value,
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await api.post('/event-plans', formData, {
                headers: { 'Content-Type': 'application/json' },
            });

            if (response.data && response.data.id) {
                navigate(`/event-plans/${response.data.id}`); // Redirect to event plan details
            }
        } catch (error) {
            console.error('Error creating event plan:', error);
        }
    };

    return (
        <Box
            component="form"
            onSubmit={handleSubmit}
            sx={{ maxWidth: 600, mx: 'auto', mt: 5, p: 3, boxShadow: 3, borderRadius: 2 }}
        >
            <Typography variant="h4" mb={3} textAlign="center">
                Create Event Plan
            </Typography>
            <TextField
                label="Name"
                name="name"
                value={formData.name}
                onChange={handleChange}
                fullWidth
                required
                margin="normal"
            />
            <TextField
                label="Description"
                name="description"
                value={formData.description}
                onChange={handleChange}
                fullWidth
                required
                margin="normal"
                multiline
                rows={3}
            />
            <TextField
                label="Event Type"
                name="event_type"
                value={formData.event_type}
                onChange={handleChange}
                select
                fullWidth
                required
                margin="normal"
            >
                {eventTypes.map((type) => (
                    <MenuItem key={type} value={type}>
                        {type}
                    </MenuItem>
                ))}
            </TextField>
            <TextField
                label="Date"
                name="date"
                value={formData.date}
                onChange={handleChange}
                type="date"
                fullWidth
                required
                margin="normal"
                InputLabelProps={{ shrink: true }}
            />
            <TextField
                label="Expected Guest Count"
                name="expected_guest_count"
                value={formData.expected_guest_count}
                onChange={handleChange}
                type="number"
                fullWidth
                required
                margin="normal"
            />
            <Button type="submit" variant="contained" color="primary" fullWidth sx={{ mt: 2 }}>
                Create Event
            </Button>
        </Box>
    );
};

export default EventPlanForm;
