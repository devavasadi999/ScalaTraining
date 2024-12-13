import React, { useState, useEffect } from "react";
import MetricsTable from "./components/MetricsTable";
import SearchBar from "./components/SearchBar";
import RefreshButton from "./components/RefreshButton";
import { Container, Typography } from "@mui/material";

function App() {
  const [metrics, setMetrics] = useState([]);
  const [filteredMetrics, setFilteredMetrics] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");

  const fetchMetrics = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/aggregated-data");
      const data = await response.json();
      setMetrics(data);
      setFilteredMetrics(data);
    } catch (error) {
      console.error("Error fetching data:", error);
    }
  };

  const handleSearch = (term) => {
    setSearchTerm(term);
    if (term) {
      setFilteredMetrics(metrics.filter((metric) => metric.sensorId.includes(term)));
    } else {
      setFilteredMetrics(metrics);
    }
  };

  useEffect(() => {
    fetchMetrics();
  }, []);

  return (
    <Container maxWidth="lg" style={{ marginTop: "20px" }}>
      <Typography variant="h4" gutterBottom>
        Sensor Readings Aggregated Metrics
      </Typography>
      <SearchBar searchTerm={searchTerm} onSearch={handleSearch} />
      <RefreshButton onRefresh={fetchMetrics} />
      <MetricsTable metrics={filteredMetrics} />
    </Container>
  );
}

export default App;
