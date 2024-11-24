import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import HomePage from './components/HomePage';
import EventPlanList from './components/EventPlanList';
import EventPlanDetails from './components/EventPlanDetails';
import ServiceTeamTasks from './components/ServiceTeamTasks';
import TaskDetails from './components/TaskDetails';
import EventPlanForm from './components/EventPlanForm';
import Layout from './components/Layout';

const App = () => {
    return (
        <Router>
            <Routes>
                {/* HomePage without Layout */}
                <Route path="/" element={<HomePage />} />

                {/* Routes with Layout */}
                <Route element={<Layout />}>
                    <Route path="/event-plans" element={<EventPlanList />} />
                    <Route path="/event-plans/:id" element={<EventPlanDetails />} />
                    <Route path="/service-team-tasks" element={<ServiceTeamTasks />} />
                    <Route path="/task-details/:id" element={<TaskDetails />} />
                    <Route path="/create-event" element={<EventPlanForm />} />
                </Route>
            </Routes>
        </Router>
    );
};

export default App;
