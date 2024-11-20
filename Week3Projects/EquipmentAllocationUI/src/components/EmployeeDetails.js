import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useParams } from 'react-router-dom';
import { Box, Typography, Grid, Card, CardContent, Button } from '@mui/material';
import ReturnEquipmentModal from './ReturnEquipmentModal';
import api from '../api';

const EmployeeDetails = () => {
    const { id } = useParams(); // Employee ID from the URL
    const [employee, setEmployee] = useState(null);
    const [allocations, setAllocations] = useState([]);
    const [selectedAllocation, setSelectedAllocation] = useState(null); // For return modal
    const [showReturnModal, setShowReturnModal] = useState(false);

    // Fetch employee details
    const fetchEmployee = async () => {
        try {
            const response = await api.get(`/employees/${id}`);
            setEmployee(response.data);
        } catch (error) {
            console.error('Error fetching employee details:', error);
        }
    };

    // Fetch equipment allocations for the employee
    const fetchAllocations = async () => {
        try {
            const response = await api.get(`/equipment-allocations/employee/${id}`);
            setAllocations(response.data);
        } catch (error) {
            console.error('Error fetching equipment allocations:', error);
        }
    };

    useEffect(() => {
        fetchEmployee();
        fetchAllocations();
    }, [id]);

    const handleReturnSuccess = () => {
        setShowReturnModal(false);
        fetchAllocations(); // Refresh allocations after return
    };

    return (
        <Box sx={{ padding: 4 }}>
            {employee ? (
                <>
                    <Typography variant="h4" sx={{ marginBottom: 2 }}>
                        Employee Details
                    </Typography>
                    <Typography variant="body1">
                        <strong>Name:</strong> {employee.name}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Email:</strong> {employee.email}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Department:</strong> {employee.department}
                    </Typography>

                    <Typography variant="h5" sx={{ marginTop: 4 }}>
                        Equipment Allocations
                    </Typography>
                    {allocations.length > 0 ? (
                        <Grid container spacing={2} sx={{ marginTop: 2 }}>
                            {allocations.map((allocation) => (
                                <Grid item xs={12} sm={6} md={4} key={allocation.equipment_allocation.id}>
                                    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                                        <CardContent sx={{ flexGrow: 1 }}>
                                            <Typography variant="h6">{allocation.equipment_type.name}</Typography>
                                            <Typography variant="body2">
                                                <strong>Equipment Code:</strong> {allocation.equipment.code}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>Purpose:</strong> {allocation.equipment_allocation.purpose}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>Status:</strong> {allocation.equipment_allocation.status}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>Allocation Date:</strong>{' '}
                                                {allocation.equipment_allocation.allocationDate}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>Expected Return Date:</strong>{' '}
                                                {allocation.equipment_allocation.expectedReturnDate || 'N/A'}
                                            </Typography>
                                            {allocation.equipment_allocation.status === 'Returned' && (
                                                <Typography variant="body2">
                                                    <strong>Actual Return Date:</strong>{' '}
                                                    {allocation.equipment_allocation.actualReturnDate || 'N/A'}
                                                </Typography>
                                            )}
                                        </CardContent>
                                        {allocation.equipment_allocation.status === 'Allocated' && (
                                            <Box sx={{ padding: 2, mt: 'auto' }}>
                                                <Button
                                                    variant="contained"
                                                    color="primary"
                                                    fullWidth
                                                    onClick={() => {
                                                        setSelectedAllocation(allocation);
                                                        setShowReturnModal(true);
                                                    }}
                                                >
                                                    Return Equipment
                                                </Button>
                                            </Box>
                                        )}
                                    </Card>
                                </Grid>
                            ))}
                        </Grid>
                    ) : (
                        <Typography variant="body2" sx={{ marginTop: 2 }}>
                            No equipment allocations found for this employee.
                        </Typography>
                    )}

                    {showReturnModal && selectedAllocation && (
                        <ReturnEquipmentModal
                            open={showReturnModal}
                            onClose={() => setShowReturnModal(false)}
                            onSuccess={handleReturnSuccess}
                            equipmentAllocation={selectedAllocation}
                        />
                    )}
                </>
            ) : (
                <Typography>Loading...</Typography>
            )}
        </Box>
    );
};

export default EmployeeDetails;
