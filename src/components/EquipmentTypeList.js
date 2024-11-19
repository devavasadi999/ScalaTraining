import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Box, Typography, Grid, Card, CardContent, Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import api from '../api';

const EquipmentTypeList = () => {
    const [equipmentTypes, setEquipmentTypes] = useState([]);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchEquipmentTypes = async () => {
            try {
                const response = await api.get('/equipment-types');
                setEquipmentTypes(response.data);
            } catch (error) {
                console.error('Error fetching equipment types:', error);
            }
        };

        fetchEquipmentTypes();
    }, []);

    return (
        <Box sx={{ padding: 4 }}>
            <Typography variant="h4" sx={{ marginBottom: 4 }}>
                Equipment Types
            </Typography>
            <Grid container spacing={2}>
                {equipmentTypes.map((type) => (
                    <Grid item xs={12} sm={6} md={4} key={type.id}>
                        <Card>
                            <CardContent>
                                <Typography variant="h6">{type.name}</Typography>
                                <Typography variant="body2">{type.description}</Typography>
                                <Button
                                    variant="contained"
                                    color="primary"
                                    sx={{ marginTop: 2 }}
                                    onClick={() => navigate(`/equipment-type/${type.id}`)}
                                >
                                    View Available Equipments
                                </Button>
                            </CardContent>
                        </Card>
                    </Grid>
                ))}
            </Grid>
        </Box>
    );
};

export default EquipmentTypeList;
