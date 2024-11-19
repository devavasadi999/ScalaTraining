import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { Box, Typography, Grid, Card, CardContent, Button } from '@mui/material';

const ServiceTeamTasks = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const serviceTeamId = searchParams.get('serviceTeamId');
    const [tasks, setTasks] = useState([]);
    const [serviceTeamName, setServiceTeamName] = useState('');

    useEffect(() => {
        const fetchServiceTeam = async () => {
            try {
                const response = await axios.get(`http://localhost:9000/serviceTeams/${serviceTeamId}`);
                setServiceTeamName(response.data.name);
            } catch (error) {
                console.error('Error fetching service team details:', error);
            }
        };

        const fetchTasks = async () => {
            try {
                const response = await axios.get(
                    `http://localhost:9000/task-assignments/service-team/${serviceTeamId}`
                );
                setTasks(response.data);
            } catch (error) {
                console.error('Error fetching service team tasks:', error);
            }
        };

        fetchServiceTeam();
        fetchTasks();
    }, [serviceTeamId]);

    return (
        <Box sx={{ padding: 4 }}>
            <Typography variant="h4" sx={{ marginBottom: 4 }}>
                Tasks for {serviceTeamName}
            </Typography>
            {tasks.length > 0 ? (
                <Grid container spacing={2}>
                    {tasks.map((task) => (
                        <Grid item xs={12} sm={6} md={4} key={task.task_assignment.id}>
                            <Card>
                                <CardContent>
                                    <Typography variant="h6">{task.task_template.name}</Typography>
                                    <Typography variant="body2">
                                        <strong>Event Plan:</strong> {task.event_plan.name}
                                    </Typography>
                                    <Typography variant="body2">
                                        <strong>Start Time:</strong> {task.task_assignment.start_time}
                                    </Typography>
                                    <Typography variant="body2">
                                        <strong>End Time:</strong> {task.task_assignment.end_time}
                                    </Typography>
                                    <Button
                                        variant="contained"
                                        color="primary"
                                        sx={{ marginTop: 2 }}
                                        onClick={() =>
                                            navigate(`/task-details/${task.task_assignment.id}?serviceTeam=true`)
                                        }
                                    >
                                        View Task
                                    </Button>
                                </CardContent>
                            </Card>
                        </Grid>
                    ))}
                </Grid>
            ) : (
                <Typography variant="body2">No tasks found for this service team.</Typography>
            )}
        </Box>
    );
};

export default ServiceTeamTasks;
