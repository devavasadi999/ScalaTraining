// AddTaskModal.js
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
    Modal,
    Box,
    Typography,
    TextField,
    Button,
    MenuItem,
    Grid,
} from '@mui/material';

const AddTaskModal = ({ open, onClose, onSuccess, eventPlanId }) => {
    const [serviceTeams, setServiceTeams] = useState([]);
    const [taskTemplates, setTaskTemplates] = useState([]);
    const [formData, setFormData] = useState({
        service_team_id: '',
        task_template_id: '',
        start_time: '',
        end_time: '',
        special_requirements: '',
        expectations: '',
    });

    const fetchServiceTeams = async () => {
        try {
            const response = await axios.get('http://localhost:9000/serviceTeams');
            setServiceTeams(response.data);
        } catch (error) {
            console.error('Error fetching service teams:', error);
        }
    };

    const fetchTaskTemplates = async (serviceTeamId) => {
        try {
            const response = await axios.get(`http://localhost:9000/taskTemplates/serviceTeam/${serviceTeamId}`);
            setTaskTemplates(response.data);
        } catch (error) {
            console.error('Error fetching task templates:', error);
        }
    };

    useEffect(() => {
        fetchServiceTeams();
    }, []);

    const handleServiceTeamChange = (e) => {
        const serviceTeamId = e.target.value;
        setFormData({ ...formData, service_team_id: serviceTeamId });
        fetchTaskTemplates(serviceTeamId);
    };

    const handleSubmit = async () => {
        try {
            // Helper function to format datetime-local input into YYYY-MM-DDTHH:mm:ss
            const formatDateTime = (dateTime) => {
                const date = new Date(dateTime);
                const year = date.getFullYear();
                const month = String(date.getMonth() + 1).padStart(2, '0');
                const day = String(date.getDate()).padStart(2, '0');
                const hours = String(date.getHours()).padStart(2, '0');
                const minutes = String(date.getMinutes()).padStart(2, '0');
                const seconds = String(date.getSeconds()).padStart(2, '0');
                return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
            };

            // Format the start_time and end_time
            const payload = {
                ...formData,
                start_time: formatDateTime(formData.start_time),
                end_time: formatDateTime(formData.end_time),
                service_team_id: parseInt(formData.service_team_id, 10), // Ensure it's an integer
                task_template_id: parseInt(formData.task_template_id, 10), // Ensure it's an integer
                event_plan_id: parseInt(eventPlanId, 10), // Ensure it's an integer
            };

            const response = await axios.post('http://localhost:9000/task-assignments', payload, {
                headers: { 'Content-Type': 'application/json' },
            });

            if (response.status === 201) {
                onSuccess(); // Callback to refresh the event details page
            }
        } catch (error) {
            console.error('Error adding task:', error);
        }
    };

    return (
        <Modal open={open} onClose={onClose}>
            <Box sx={{ backgroundColor: 'white', padding: 4, maxWidth: 600, margin: 'auto', marginTop: '10%' }}>
                <Typography variant="h6" sx={{ marginBottom: 2 }}>
                    Add Task
                </Typography>
                <Grid container spacing={2}>
                    <Grid item xs={12}>
                        <TextField
                            select
                            fullWidth
                            label="Service Team"
                            value={formData.service_team_id}
                            onChange={handleServiceTeamChange}
                        >
                            {serviceTeams.map((team) => (
                                <MenuItem key={team.id} value={team.id}>
                                    {team.name}
                                </MenuItem>
                            ))}
                        </TextField>
                    </Grid>
                    <Grid item xs={12}>
                        <TextField
                            select
                            fullWidth
                            label="Task Template"
                            value={formData.task_template_id}
                            onChange={(e) =>
                                setFormData({ ...formData, task_template_id: e.target.value })
                            }
                            disabled={!formData.service_team_id}
                        >
                            {taskTemplates.map((template) => (
                                <MenuItem key={template.id} value={template.id}>
                                    {template.description}
                                </MenuItem>
                            ))}
                        </TextField>
                    </Grid>
                    <Grid item xs={12}>
                        <TextField
                            fullWidth
                            type="datetime-local"
                            label="Start Time"
                            value={formData.start_time}
                            onChange={(e) =>
                                setFormData({ ...formData, start_time: e.target.value })
                            }
                            InputLabelProps={{ shrink: true }} // Ensures label does not overlap with input
                            placeholder="" // Removes the default placeholder
                        />
                    </Grid>
                    <Grid item xs={12}>
                        <TextField
                            fullWidth
                            type="datetime-local"
                            label="End Time"
                            value={formData.end_time}
                            onChange={(e) =>
                                setFormData({ ...formData, end_time: e.target.value })
                            }
                            InputLabelProps={{ shrink: true }} // Ensures label does not overlap with input
                            placeholder="" // Removes the default placeholder
                        />
                    </Grid>
                    <Grid item xs={12}>
                        <TextField
                            fullWidth
                            label="Special Requirements"
                            value={formData.special_requirements}
                            onChange={(e) =>
                                setFormData({ ...formData, special_requirements: e.target.value })
                            }
                        />
                    </Grid>
                    <Grid item xs={12}>
                        <TextField
                            fullWidth
                            label="Expectations"
                            value={formData.expectations}
                            onChange={(e) =>
                                setFormData({ ...formData, expectations: e.target.value })
                            }
                        />
                    </Grid>
                </Grid>
                <Button
                    variant="contained"
                    color="primary"
                    sx={{ marginTop: 2 }}
                    onClick={handleSubmit}
                >
                    Add Task
                </Button>
            </Box>
        </Modal>
    );
};

export default AddTaskModal;
