import React, { useEffect, useState } from 'react';
import { Box, Typography, Grid, Card, CardContent, Button, Select, MenuItem } from '@mui/material';
import api from '../api';

const MaintenanceTeam = () => {
    const [repairRequests, setRepairRequests] = useState([]);
    const [statuses, setStatuses] = useState({}); // To track status changes for each repair

    const getAuthorizationHeaders = () => {
        const token = localStorage.getItem('token');
        if (!token) {
            console.error('No token found. User might not be logged in.');
            return null;
        }
        return { Authorization: `Bearer ${token}` };
    };

    // Fetch all repair requests with details
    const fetchRepairRequests = async () => {
        try {
            const headers = getAuthorizationHeaders();
            if (!headers) return;

            const response = await api.get('/equipment-repairs', { headers });
            // Sort by ID
            const sortedRepairs = response.data.sort((a, b) => a.equipment_repair.id - b.equipment_repair.id);
            setRepairRequests(sortedRepairs);
        } catch (error) {
            console.error('Error fetching repair requests:', error);
        }
    };

    // Handle status update
    const handleStatusChange = async (repairId, newStatus) => {
        try {
            const headers = getAuthorizationHeaders();
            if (!headers) return;

            await api.patch(
                `/equipment_repair/${repairId}`,
                { status: newStatus },
                { headers }
            );
            setStatuses((prev) => ({ ...prev, [repairId]: newStatus }));
            fetchRepairRequests(); // Refresh the list
        } catch (error) {
            console.error('Error updating repair status:', error);
        }
    };

    useEffect(() => {
        fetchRepairRequests();
    }, []);

    return (
        <Box sx={{ padding: 4 }}>
            <Typography variant="h4" sx={{ marginBottom: 4 }}>
                Maintenance Team - Repair Requests
            </Typography>
            {repairRequests.length > 0 ? (
                <Grid container spacing={2}>
                    {repairRequests.map((repair) => (
                        <Grid item xs={12} sm={6} md={4} key={repair.equipment_repair.id}>
                            <Card
                                sx={{
                                    height: '100%',
                                    display: 'flex',
                                    flexDirection: 'column',
                                    justifyContent: 'space-between',
                                }}
                            >
                                <CardContent>
                                    <Typography variant="h6">{repair.equipment_type.name}</Typography>
                                    <Typography variant="body2">
                                        <strong>Equipment Code:</strong> {repair.equipment.code}
                                    </Typography>
                                    <Typography variant="body2">
                                        <strong>Description:</strong> {repair.equipment_type.description}
                                    </Typography>
                                    <Typography variant="body2">
                                        <strong>Repair Description:</strong> {repair.equipment_repair.repairDescription}
                                    </Typography>
                                    <Typography variant="body2">
                                        <strong>Status:</strong>{' '}
                                        {statuses[repair.equipment_repair.id] || repair.equipment_repair.status}
                                    </Typography>
                                </CardContent>
                                <Box sx={{ padding: 2 }}>
                                    <Select
                                        fullWidth
                                        value={statuses[repair.equipment_repair.id] || repair.equipment_repair.status}
                                        onChange={(e) =>
                                            handleStatusChange(repair.equipment_repair.id, e.target.value)
                                        }
                                        sx={{ marginTop: 2 }}
                                    >
                                        <MenuItem value="Pending">Pending</MenuItem>
                                        <MenuItem value="InProgress">In Progress</MenuItem>
                                        <MenuItem value="Completed">Completed</MenuItem>
                                    </Select>
                                </Box>
                            </Card>
                        </Grid>
                    ))}
                </Grid>
            ) : (
                <Typography>No repair requests found.</Typography>
            )}
        </Box>
    );
};

export default MaintenanceTeam;
