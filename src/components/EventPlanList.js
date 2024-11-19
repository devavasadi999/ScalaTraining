import React, { useEffect, useState } from 'react';
import axios from 'axios';
import {
    Box,
    Typography,
    Button,
    Card,
    CardContent,
    Grid,
} from '@mui/material';
import { Link } from 'react-router-dom';
import api from '../api';

const EventPlanList = () => {
    const [eventPlans, setEventPlans] = useState([]);

    // Fetch event plans from the backend
    const fetchEventPlans = async () => {
        try {
            const response = await api.get('/event-plans');
            setEventPlans(response.data || []);
        } catch (error) {
            console.error('Error fetching event plans:', error);
        }
    };

    useEffect(() => {
        fetchEventPlans();
    }, []);

    return (
        <Box sx={{ mt: 5, textAlign: 'center' }}>
            <Typography variant="h3" gutterBottom>
                Event Plans
            </Typography>
            <Box sx={{ display: 'flex', justifyContent: 'center', mb: 4 }}>
                <Button
                    component={Link}
                    to="/create-event"
                    variant="contained"
                    color="primary"
                >
                    Create New Event
                </Button>
            </Box>
            <Grid container spacing={3}>
                {eventPlans.map((eventPlan) => (
                    <Grid item xs={12} sm={6} md={4} key={eventPlan.id}>
                        <Card>
                            <CardContent>
                                <Typography variant="h5" gutterBottom>
                                    {eventPlan.name}
                                </Typography>
                                <Typography variant="body1" gutterBottom>
                                    Event Type: {eventPlan.event_type}
                                </Typography>
                                <Typography variant="body2" gutterBottom>
                                    Date: {eventPlan.date}
                                </Typography>
                                <Button
                                    component={Link}
                                    to={`/event-plans/${eventPlan.id}`}
                                    variant="outlined"
                                    color="primary"
                                    sx={{ mt: 2 }}
                                >
                                    View Details
                                </Button>
                            </CardContent>
                        </Card>
                    </Grid>
                ))}
            </Grid>
        </Box>
    );
};

export default EventPlanList;
