import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import AddTaskModal from './AddTaskModal';
import { Box, Typography, Button, Grid, Card, CardContent } from '@mui/material';
import api from '../api';

const EventPlanDetails = () => {
    const { id } = useParams(); // Event Plan ID
    const navigate = useNavigate(); // Navigation to other pages
    const [eventPlan, setEventPlan] = useState(null);
    const [tasks, setTasks] = useState([]);
    const [showAddTaskModal, setShowAddTaskModal] = useState(false);

    // Fetch the event plan details
    const fetchEventPlan = async () => {
        try {
            const token = localStorage.getItem('token'); // Retrieve token from localStorage
            if (!token) {
                console.error('No token found. User might not be logged in.');
                return;
            }

            const response = await api.get(`/event-plans/${id}`, {
                headers: {
                    Authorization: `Bearer ${token}`, // Include Authorization header
                },
            });
            setEventPlan(response.data.eventPlan);
        } catch (error) {
            console.error('Error fetching event plan:', error);
        }
    };

    // Fetch the tasks associated with the event plan
    const fetchTasks = async () => {
        try {
            const token = localStorage.getItem('token'); // Retrieve token from localStorage
            if (!token) {
                console.error('No token found. User might not be logged in.');
                return;
            }

            const response = await api.get(`/task-assignments/event-plan/${id}`, {
                headers: {
                    Authorization: `Bearer ${token}`, // Include Authorization header
                },
            });
            setTasks(response.data);
        } catch (error) {
            console.error('Error fetching tasks:', error);
        }
    };

    const handleAddTaskSuccess = () => {
        setShowAddTaskModal(false);
        fetchTasks(); // Refresh tasks after adding a new one
    };

    useEffect(() => {
        fetchEventPlan();
        fetchTasks();
    }, [id]);

    return (
        <Box sx={{ padding: 4 }}>
            {eventPlan ? (
                <>
                    <Typography variant="h4" sx={{ marginBottom: 2 }}>
                        Event Details
                    </Typography>
                    <Typography variant="body1">
                        <strong>Name:</strong> {eventPlan.name}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Description:</strong> {eventPlan.description}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Event Type:</strong> {eventPlan.event_type}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Date:</strong> {eventPlan.date}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Expected Guest Count:</strong> {eventPlan.expected_guest_count}
                    </Typography>

                    <Button
                        variant="contained"
                        color="primary"
                        sx={{ marginTop: 2 }}
                        onClick={() => setShowAddTaskModal(true)}
                    >
                        Add Task
                    </Button>

                    <Typography variant="h5" sx={{ marginTop: 4 }}>
                        Tasks
                    </Typography>
                    {tasks.length > 0 ? (
                        <Grid container spacing={2} sx={{ marginTop: 2 }}>
                            {tasks.map((task) => (
                                <Grid item xs={12} sm={6} md={4} key={task.task_assignment.id}>
                                    <Card
                                        onClick={() => navigate(`/task-details/${task.task_assignment.id}`)} // Navigate to task details
                                        sx={{ cursor: 'pointer' }}
                                    >
                                        <CardContent>
                                            <Typography variant="h6">{task.task_template.name}</Typography>
                                            <Typography variant="body2">
                                                <strong>Service Team:</strong> {task.service_team.name}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>Start Time:</strong> {new Date(task.task_assignment.start_time).toLocaleString()}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>End Time:</strong> {new Date(task.task_assignment.end_time).toLocaleString()}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                            ))}
                        </Grid>
                    ) : (
                        <Typography variant="body2" sx={{ marginTop: 2 }}>
                            No tasks added.
                        </Typography>
                    )}

                    {showAddTaskModal && (
                        <AddTaskModal
                            open={showAddTaskModal}
                            onClose={() => setShowAddTaskModal(false)}
                            onSuccess={handleAddTaskSuccess}
                            eventPlanId={id}
                        />
                    )}
                </>
            ) : (
                <Typography>Loading event plan details...</Typography>
            )}
        </Box>
    );
};

export default EventPlanDetails;