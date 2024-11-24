import React, { useState } from 'react';
import { Modal, Box, TextField, Button, Typography } from '@mui/material';
import api from '../api';

const RaiseIssueModal = ({ open, onClose, onSuccess, taskAssignmentId }) => {
    const [problem, setProblem] = useState('');
    const [error, setError] = useState('');

    const handleSubmit = async () => {
        if (!problem.trim()) {
            setError('Problem description is required');
            return;
        }

        try {
            const token = localStorage.getItem('token'); // Retrieve the token from localStorage
            if (!token) {
                console.error('No token found. User might not be logged in.');
                return;
            }

            await api.post(
                '/taskIssues',
                {
                    taskAssignmentId: parseInt(taskAssignmentId),
                    problem,
                    status: 'Pending',
                },
                {
                    headers: {
                        Authorization: `Bearer ${token}`, // Include Authorization header
                    },
                }
            );
            onSuccess(); // Callback on success
        } catch (error) {
            console.error('Error creating issue:', error);
        }
    };

    return (
        <Modal open={open} onClose={onClose}>
            <Box
                sx={{
                    padding: 4,
                    backgroundColor: 'white',
                    borderRadius: 2,
                    maxWidth: 400,
                    margin: 'auto',
                    marginTop: '10%',
                }}
            >
                <Typography variant="h6" sx={{ marginBottom: 2 }}>
                    Raise an Issue
                </Typography>
                <TextField
                    fullWidth
                    label="Problem Description"
                    multiline
                    rows={4}
                    value={problem}
                    onChange={(e) => setProblem(e.target.value)}
                    error={!!error}
                    helperText={error}
                />
                <Button variant="contained" color="primary" sx={{ marginTop: 2 }} onClick={handleSubmit}>
                    Submit
                </Button>
            </Box>
        </Modal>
    );
};

export default RaiseIssueModal;
