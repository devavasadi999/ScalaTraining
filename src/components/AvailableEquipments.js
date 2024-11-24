import React, { useEffect, useState } from 'react';
import { Box, Typography, Grid, Card, CardContent, Button } from '@mui/material';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api';

const AvailableEquipments = () => {
    const { id } = useParams();
    const [equipments, setEquipments] = useState([]);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchEquipments = async () => {
            try {
                const token = localStorage.getItem('token'); // Retrieve token from localStorage
                if (!token) {
                    console.error('No token found. User might not be logged in.');
                    return;
                }

                const response = await api.get(`/equipments/available/${id}`, {
                    headers: {
                        Authorization: `Bearer ${token}`, // Include Authorization header
                    },
                });
                setEquipments(response.data);
            } catch (error) {
                console.error('Error fetching available equipment:', error);
            }
        };

        fetchEquipments();
    }, [id]);

    return (
        <Box sx={{ padding: 4 }}>
            <Typography variant="h4" sx={{ marginBottom: 4 }}>
                Available Equipments
            </Typography>
            <Grid container spacing={2}>
                {equipments.map(({ equipment, equipment_type: equipmentType }) => (
                    <Grid item xs={12} sm={6} md={4} key={equipment.id}>
                        <Card>
                            <CardContent>
                                <Typography variant="h6">Code: {equipment.code}</Typography>
                                <Typography variant="body2">Type: {equipmentType.name}</Typography>
                                <Button
                                    variant="contained"
                                    color="primary"
                                    sx={{ marginTop: 2 }}
                                    onClick={() => navigate(`/equipment/${equipment.id}`)}
                                >
                                    View
                                </Button>
                            </CardContent>
                        </Card>
                    </Grid>
                ))}
            </Grid>
        </Box>
    );
};

export default AvailableEquipments;
