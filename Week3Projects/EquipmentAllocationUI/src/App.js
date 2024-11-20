import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar';
import EmployeeList from './components/EmployeeList';
import EquipmentTypeList from './components/EquipmentTypeList';
import EmployeeDetails from './components/EmployeeDetails';
import AvailableEquipments from './components/AvailableEquipments';
import EquipmentDetails from './components/EquipmentDetails';
import Dashboard from "./components/Dashboard";
import MaintenanceTeam from "./components/MaintenanceTeam";
import ReceptionStaff from "./components/ReceptionStaff";

function App() {
  return (
      <Router>
        <Navbar />
        <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/maintenance" element={<MaintenanceTeam />} />
            <Route path="/reception" element={<ReceptionStaff />} />
            <Route path="/equipment-details/:id" element={<EquipmentDetails />} />
            <Route path="/employees" element={<EmployeeList />} />
          <Route path="/employees/:id" element={<EmployeeDetails />} />
          <Route path="/equipment-type/:id" element={<AvailableEquipments />} />
          <Route path="/equipment/:id" element={<EquipmentDetails />} />
            <Route path="/equipment-types" element={<EquipmentTypeList />} />
        </Routes>
      </Router>
  );
}

export default App;
