import React, { useState } from 'react';
import {
    Modal,
    Box,
    TextField,
    Button,
    Typography,
    Checkbox,
    FormControlLabel,
} from '@mui/material';
import api from '../api';

const ReturnEquipmentModal = ({ open, onClose, onSuccess, equipmentAllocation }) => {
    const [isServiceRequired, setIsServiceRequired] = useState(false); // Checkbox state
    const [serviceDescription, setServiceDescription] = useState(''); // Service description
    const [error, setError] = useState('');

    const getAuthorizationHeaders = () => {
        const token = localStorage.getItem('token');
        if (!token) {
            console.error('No token found. User might not be logged in.');
            return null;
        }
        return { Authorization: `Bearer ${token}` };
    };

    const handleSubmit = async () => {
        try {
            const headers = getAuthorizationHeaders();
            if (!headers) {
                setError('Unauthorized. Please log in.');
                return;
            }

            // Prepare data for the return request
            const payload = {
                status: 'Returned',
            };

            // PATCH call to update equipment allocation
            await api.patch(
                `/equipment_allocation/${equipmentAllocation.equipment_allocation.id}`,
                payload,
                { headers: { ...headers, 'Content-Type': 'application/json' } }
            );

            // If service is required, create a repair record
            if (isServiceRequired) {
                const repairPayload = {
                    equipment_id: equipmentAllocation.equipment.id,
                    service_description: serviceDescription.trim(),
                };

                await api.post(
                    '/equipment_repair',
                    repairPayload,
                    { headers: { ...headers, 'Content-Type': 'application/json' } }
                );
            }

            onSuccess(); // Trigger success callback
            onClose(); // Close the modal
        } catch (error) {
            console.error('Error returning equipment or creating repair request:', error);
            setError('Failed to process the request. Please try again.');
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
                    boxShadow: 3,
                }}
            >
                <Typography variant="h6" sx={{ marginBottom: 2 }}>
                    Return Equipment
                </Typography>
                <Typography variant="body2" sx={{ marginBottom: 2 }}>
                    <strong>Equipment:</strong> {equipmentAllocation.equipment.code}
                </Typography>
                <FormControlLabel
                    control={
                        <Checkbox
                            checked={isServiceRequired}
                            onChange={(e) => setIsServiceRequired(e.target.checked)}
                        />
                    }
                    label="Service Required"
                />
                {isServiceRequired && (
                    <TextField
                        fullWidth
                        label="Service Description"
                        multiline
                        rows={3}
                        value={serviceDescription}
                        onChange={(e) => setServiceDescription(e.target.value)}
                        sx={{ marginBottom: 2 }}
                    />
                )}
                {error && (
                    <Typography variant="body2" color="error" sx={{ marginBottom: 2 }}>
                        {error}
                    </Typography>
                )}
                <Button variant="contained" color="primary" onClick={handleSubmit} fullWidth>
                    Return
                </Button>
                <Button
                    variant="outlined"
                    color="secondary"
                    onClick={onClose}
                    fullWidth
                    sx={{ marginTop: 1 }}
                >
                    Cancel
                </Button>
            </Box>
        </Modal>
    );
};

export default ReturnEquipmentModal;
