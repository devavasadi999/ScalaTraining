import React, { useEffect, useState } from 'react';
import { useParams, useLocation } from 'react-router-dom';
import {
    Box,
    Typography,
    Button,
    Grid,
    Card,
    CardContent,
    Select,
    MenuItem,
    FormControl,
    InputLabel,
} from '@mui/material';
import RaiseIssueModal from './RaiseIssueModal';
import api from '../api';

const TaskDetails = () => {
    const { id } = useParams(); // Task Assignment ID
    const location = useLocation();
    const isServiceTeam = new URLSearchParams(location.search).get('serviceTeam') === 'true';

    const [taskDetails, setTaskDetails] = useState(null); // Task Assignment Details
    const [issues, setIssues] = useState([]); // List of Task Issues
    const [status, setStatus] = useState(''); // Current Task Status
    const [showRaiseIssueModal, setShowRaiseIssueModal] = useState(false);

    // Fetch Task Details
    const fetchTaskDetails = async () => {
        try {
            const token = localStorage.getItem('token'); // Retrieve token from localStorage
            if (!token) {
                console.error('No token found. User might not be logged in.');
                return;
            }

            const response = await api.get(`/task-assignments/${id}`, {
                headers: {
                    Authorization: `Bearer ${token}`, // Include Authorization header
                },
            });
            const data = response.data;
            setTaskDetails({
                taskAssignment: data.task_assignment,
                taskTemplate: data.task_template,
                serviceTeam: data.service_team,
                eventPlan: data.event_plan,
            });
            setStatus(data.task_assignment.status); // Set the task status
        } catch (error) {
            console.error('Error fetching task details:', error);
        }
    };

    // Fetch Task Issues
    const fetchTaskIssues = async () => {
        try {
            const token = localStorage.getItem('token'); // Retrieve token from localStorage
            if (!token) {
                console.error('No token found. User might not be logged in.');
                return;
            }

            const response = await api.get(`/taskIssues/taskAssignment/${id}`, {
                headers: {
                    Authorization: `Bearer ${token}`, // Include Authorization header
                },
            });
            setIssues(response.data.map((issue) => issue.task_issue));
        } catch (error) {
            console.error('Error fetching task issues:', error);
        }
    };

    useEffect(() => {
        fetchTaskDetails();
        fetchTaskIssues();
    }, [id]);

    // Handle Issue Resolution
    const handleResolveIssue = async (issueId) => {
        try {
            const token = localStorage.getItem('token'); // Retrieve token from localStorage
            if (!token) {
                console.error('No token found. User might not be logged in.');
                return;
            }

            await api.patch(`/taskIssues/${issueId}`, { status: 'Resolved' }, {
                headers: {
                    Authorization: `Bearer ${token}`, // Include Authorization header
                },
            });
            fetchTaskIssues(); // Refresh issues after resolving
        } catch (error) {
            console.error('Error resolving issue:', error);
        }
    };

    // Handle Status Change
    const handleStatusChange = async (newStatus) => {
        try {
            const token = localStorage.getItem('token'); // Retrieve token from localStorage
            if (!token) {
                console.error('No token found. User might not be logged in.');
                return;
            }

            await api.patch(`/task-assignments/${id}`, { status: newStatus }, {
                headers: {
                    Authorization: `Bearer ${token}`, // Include Authorization header
                },
            });
            setStatus(newStatus);
            fetchTaskDetails(); // Refresh task details
        } catch (error) {
            console.error('Error updating task status:', error);
        }
    };

    // Handle Success after Raising Issue
    const handleRaiseIssueSuccess = () => {
        setShowRaiseIssueModal(false);
        fetchTaskIssues(); // Refresh issues after adding
    };

    return (
        <Box sx={{ padding: 4 }}>
            {taskDetails ? (
                <>
                    <Typography variant="h4" sx={{ marginBottom: 2 }}>
                        Task Details
                    </Typography>
                    <Typography variant="body1">
                        <strong>Event Plan:</strong> {taskDetails.eventPlan.name}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Task Name:</strong> {taskDetails.taskTemplate.name}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Service Team:</strong> {taskDetails.serviceTeam.name}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Start Time:</strong>{' '}
                        {new Date(taskDetails.taskAssignment.start_time).toLocaleString()}
                    </Typography>
                    <Typography variant="body1">
                        <strong>End Time:</strong>{' '}
                        {new Date(taskDetails.taskAssignment.end_time).toLocaleString()}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Expectations:</strong> {taskDetails.taskAssignment.expectations || 'N/A'}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Special Requirements:</strong>{' '}
                        {taskDetails.taskAssignment.special_requirements || 'N/A'}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Status:</strong> {status}
                    </Typography>

                    {isServiceTeam && (
                        <>
                            <Button
                                variant="contained"
                                color="primary"
                                sx={{ marginTop: 2 }}
                                onClick={() => setShowRaiseIssueModal(true)}
                            >
                                Raise Issue
                            </Button>
                            <Box sx={{ maxWidth: 300, marginTop: 2 }}>
                                <FormControl fullWidth>
                                    <InputLabel id="status-select-label">Change Status</InputLabel>
                                    <Select
                                        labelId="status-select-label"
                                        value={status}
                                        onChange={(e) => handleStatusChange(e.target.value)}
                                    >
                                        <MenuItem value="TODO">TODO</MenuItem>
                                        <MenuItem value="INPROGRESS">IN PROGRESS</MenuItem>
                                        <MenuItem value="COMPLETED">COMPLETED</MenuItem>
                                    </Select>
                                </FormControl>
                            </Box>
                        </>
                    )}

                    <Typography variant="h5" sx={{ marginTop: 4 }}>
                        Task Issues
                    </Typography>
                    {issues.length > 0 ? (
                        <Grid container spacing={2} sx={{ marginTop: 2 }}>
                            {issues.map((issue) => (
                                <Grid item xs={12} sm={6} md={4} key={issue.id}>
                                    <Card>
                                        <CardContent>
                                            <Typography variant="body1">
                                                <strong>Problem:</strong> {issue.problem}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>Status:</strong> {issue.status}
                                            </Typography>
                                            {isServiceTeam && issue.status !== 'Resolved' && (
                                                <Button
                                                    variant="outlined"
                                                    color="secondary"
                                                    sx={{ marginTop: 1 }}
                                                    onClick={() => handleResolveIssue(issue.id)}
                                                >
                                                    Resolve
                                                </Button>
                                            )}
                                        </CardContent>
                                    </Card>
                                </Grid>
                            ))}
                        </Grid>
                    ) : (
                        <Typography variant="body2" sx={{ marginTop: 2 }}>
                            No issues found.
                        </Typography>
                    )}

                    {showRaiseIssueModal && (
                        <RaiseIssueModal
                            open={showRaiseIssueModal}
                            onClose={() => setShowRaiseIssueModal(false)}
                            onSuccess={handleRaiseIssueSuccess}
                            taskAssignmentId={id}
                        />
                    )}
                </>
            ) : (
                <Typography>Loading...</Typography>
            )}
        </Box>
    );
};

export default TaskDetails;
