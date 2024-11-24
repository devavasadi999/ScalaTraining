import React, { useEffect, useState } from 'react';
import {
    Box,
    Typography,
    Card,
    CardContent,
    Button,
    Grid,
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    OutlinedInput,
    TextField,
} from '@mui/material';
import { useParams } from 'react-router-dom';
import api from '../api';

const EquipmentDetails = () => {
    const { id } = useParams(); // Equipment ID
    const [equipment, setEquipment] = useState(null); // Equipment Details
    const [employees, setEmployees] = useState([]); // List of Employees
    const [allocations, setAllocations] = useState([]); // Equipment Allocations
    const [repairs, setRepairs] = useState([]); // Equipment Repairs
    const [selectedEmployee, setSelectedEmployee] = useState(''); // Selected Employee for Allocation
    const [search, setSearch] = useState(''); // Employee Search
    const [purpose, setPurpose] = useState(''); // Purpose for Allocation
    const [expectedReturnDate, setExpectedReturnDate] = useState('');

    const getAuthorizationHeaders = () => {
        const token = localStorage.getItem('token');
        if (!token) {
            console.error('No token found. User might not be logged in.');
            return null;
        }
        return { Authorization: `Bearer ${token}` };
    };

    // Fetch Equipment Details
    const fetchEquipmentDetails = async () => {
        try {
            const headers = getAuthorizationHeaders();
            if (!headers) return;

            const response = await api.get(`/equipment/${id}`, { headers });
            setEquipment(response.data);
        } catch (error) {
            console.error('Error fetching equipment details:', error);
        }
    };

    // Fetch Equipment Allocations
    const fetchAllocations = async () => {
        try {
            const headers = getAuthorizationHeaders();
            if (!headers) return;

            const response = await api.get(`/equipment-allocations/equipment/${id}`, { headers });
            const sortedAllocations = response.data.sort((a, b) =>
                new Date(a.equipment_allocation.allocationDate) - new Date(b.equipment_allocation.allocationDate)
            );
            setAllocations(sortedAllocations);
        } catch (error) {
            console.error('Error fetching equipment allocations:', error);
        }
    };

    // Fetch All Employees
    const fetchEmployees = async () => {
        try {
            const headers = getAuthorizationHeaders();
            if (!headers) return;

            const response = await api.get('/employees', { headers });
            setEmployees(response.data);
        } catch (error) {
            console.error('Error fetching employees:', error);
        }
    };

    // Fetch Equipment Repairs
    const fetchRepairs = async () => {
        try {
            const headers = getAuthorizationHeaders();
            if (!headers) return;

            const response = await api.get(`/equipment-repair/equipment/${id}`, { headers });
            setRepairs(response.data);
        } catch (error) {
            console.error('Error fetching equipment repairs:', error);
        }
    };

    // Handle Equipment Allocation
    const handleAllocateEquipment = async () => {
        if (!selectedEmployee || !purpose.trim() || !expectedReturnDate) {
            alert('Please select an employee, enter a purpose, and provide an expected return date.');
            return;
        }

        try {
            const headers = getAuthorizationHeaders();
            if (!headers) return;

            const payload = {
                employee_id: parseInt(selectedEmployee),
                equipment_id: parseInt(id),
                purpose: purpose.trim(),
                expectedReturnDate, // Uses the date field value directly
            };
            await api.post('/equipment_allocation', payload, {
                headers: { ...headers, 'Content-Type': 'application/json' },
            });
            alert('Equipment allocated successfully!');
            fetchAllocations(); // Refresh allocations
        } catch (error) {
            console.error('Error allocating equipment:', error);
            alert('Failed to allocate equipment. Please try again.');
        }
    };

    useEffect(() => {
        fetchEquipmentDetails();
        fetchAllocations();
        fetchEmployees();
        fetchRepairs();
    }, [id]);

    // Filter employees based on search input
    const filteredEmployees = employees.filter(
        (employee) =>
            employee.name.toLowerCase().includes(search.toLowerCase()) ||
            employee.department.toLowerCase().includes(search.toLowerCase())
    );

    const hasAllocated = allocations.some((allocation) => allocation.equipment_allocation.status === 'Allocated');
    const hasPendingRepairs = repairs.some((repair) => repair.status !== 'Completed');

    return (
        <Box sx={{ padding: 4 }}>
            {equipment ? (
                <>
                    <Typography variant="h4" sx={{ marginBottom: 2 }}>
                        Equipment Details
                    </Typography>
                    <Typography variant="body1">
                        <strong>Equipment Code:</strong> {equipment.equipment.code}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Equipment Type:</strong> {equipment.equipmentType.name}
                    </Typography>
                    <Typography variant="body1">
                        <strong>Description:</strong> {equipment.equipmentType.description}
                    </Typography>

                    {/* Equipment Allocations */}
                    <Typography variant="h5" sx={{ marginTop: 4 }}>
                        Equipment Allocations
                    </Typography>
                    {allocations.length > 0 ? (
                        <Grid container spacing={2} sx={{ marginTop: 2 }}>
                            {allocations.map((allocation) => (
                                <Grid item xs={12} sm={6} md={4} key={allocation.equipment_allocation.id}>
                                    <Card sx={{ height: '100%' }}>
                                        <CardContent>
                                            <Typography variant="body2">
                                                <strong>Employee:</strong> {allocation.employee.name}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>Status:</strong> {allocation.equipment_allocation.status}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>Purpose:</strong> {allocation.equipment_allocation.purpose}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>Allocation Date:</strong>{' '}
                                                {allocation.equipment_allocation.allocationDate}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>
                                                    {allocation.equipment_allocation.status === 'Allocated'
                                                        ? 'Expected Return Date:'
                                                        : 'Actual Return Date:'}
                                                </strong>{' '}
                                                {allocation.equipment_allocation.status === 'Allocated'
                                                    ? allocation.equipment_allocation.expectedReturnDate
                                                    : allocation.equipment_allocation.actualReturnDate || 'N/A'}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                            ))}
                        </Grid>
                    ) : (
                        <Typography variant="body2" sx={{ marginTop: 2 }}>
                            No allocations found for this equipment.
                        </Typography>
                    )}

                    {/* Repair Records */}
                    <Typography variant="h5" sx={{ marginTop: 4 }}>
                        Repair Records
                    </Typography>
                    {repairs.length > 0 ? (
                        <Grid container spacing={2} sx={{ marginTop: 2 }}>
                            {repairs.map((repair) => (
                                <Grid item xs={12} sm={6} md={4} key={repair.id}>
                                    <Card>
                                        <CardContent>
                                            <Typography variant="body1">
                                                <strong>Repair Description:</strong> {repair.repairDescription}
                                            </Typography>
                                            <Typography variant="body2">
                                                <strong>Status:</strong> {repair.status}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                </Grid>
                            ))}
                        </Grid>
                    ) : (
                        <Typography variant="body2" sx={{ marginTop: 2 }}>
                            No repair records found for this equipment.
                        </Typography>
                    )}

                    {/* Allocate Equipment */}
                    {!hasAllocated && !hasPendingRepairs && (
                        <Box sx={{ marginTop: 4 }}>
                            <Typography variant="h5" sx={{ marginBottom: 2 }}>
                                Allocate Equipment
                            </Typography>
                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 2,
                                    flexWrap: 'wrap',
                                    maxWidth: 1000,
                                }}
                            >
                                <TextField
                                    placeholder="Search for Employee"
                                    fullWidth
                                    value={search}
                                    onChange={(e) => setSearch(e.target.value)}
                                    sx={{ flex: 2, marginBottom: 2, minWidth: 300 }}
                                />
                                <FormControl sx={{ flex: 2, minWidth: 300, marginBottom: 2 }}>
                                    <InputLabel id="employee-select-label">Select Employee</InputLabel>
                                    <Select
                                        labelId="employee-select-label"
                                        value={selectedEmployee}
                                        onChange={(e) => setSelectedEmployee(e.target.value)}
                                        input={<OutlinedInput label="Select Employee" />}
                                    >
                                        {filteredEmployees.map((employee) => (
                                            <MenuItem key={employee.id} value={employee.id}>
                                                {employee.name} ({employee.department})
                                            </MenuItem>
                                        ))}
                                    </Select>
                                </FormControl>
                                <TextField
                                    placeholder="Enter Purpose"
                                    fullWidth
                                    value={purpose}
                                    onChange={(e) => setPurpose(e.target.value)}
                                    sx={{ flex: 2, marginBottom: 2, minWidth: 300 }}
                                />
                                <TextField
                                    type="date"
                                    fullWidth
                                    label="Expected Return Date"
                                    InputLabelProps={{
                                        shrink: true,
                                    }}
                                    value={expectedReturnDate}
                                    onChange={(e) => setExpectedReturnDate(e.target.value)}
                                    sx={{ flex: 2, marginBottom: 2, minWidth: 300 }}
                                />
                                <Button
                                    variant="contained"
                                    color="primary"
                                    onClick={handleAllocateEquipment}
                                    disabled={!selectedEmployee || !purpose.trim() || !expectedReturnDate}
                                    sx={{
                                        height: 56,
                                        width: 250,
                                        flexShrink: 0,
                                    }}
                                >
                                    Allocate Equipment
                                </Button>
                            </Box>
                        </Box>
                    )}
                </>
            ) : (
                <Typography>Loading...</Typography>
            )}
        </Box>
    );
};

export default EquipmentDetails;
