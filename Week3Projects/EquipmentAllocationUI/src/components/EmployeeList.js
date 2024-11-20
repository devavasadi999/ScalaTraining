import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Box, Typography, Grid, Card, CardContent, Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import api from '../api';

const EmployeeList = () => {
    const [employees, setEmployees] = useState([]);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchEmployees = async () => {
            try {
                const response = await api.get('/employees');
                setEmployees(response.data);
            } catch (error) {
                console.error('Error fetching employees:', error);
            }
        };

        fetchEmployees();
    }, []);

    return (
        <Box sx={{ padding: 4 }}>
            <Typography variant="h4" sx={{ marginBottom: 4 }}>
                Employees
            </Typography>
            <Grid container spacing={2}>
                {employees.map((employee) => (
                    <Grid item xs={12} sm={6} md={4} key={employee.id}>
                        <Card>
                            <CardContent>
                                <Typography variant="h6">{employee.name}</Typography>
                                <Typography variant="body2">Department: {employee.department}</Typography>
                                <Button
                                    variant="contained"
                                    color="primary"
                                    sx={{ marginTop: 2 }}
                                    onClick={() => navigate(`/employees/${employee.id}`)}
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

export default EmployeeList;
