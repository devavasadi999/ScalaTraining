import React from "react";
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper } from "@mui/material";

const MetricsTable = ({ metrics }) => {
  return (
    <TableContainer component={Paper} style={{ marginTop: "20px" }}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Sensor ID</TableCell>
            <TableCell align="right">Average Temperature</TableCell>
            <TableCell align="right">Average Humidity</TableCell>
            <TableCell align="right">Min Temperature</TableCell>
            <TableCell align="right">Max Temperature</TableCell>
            <TableCell align="right">Min Humidity</TableCell>
            <TableCell align="right">Max Humidity</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {metrics.map((metric) => (
            <TableRow key={metric.sensorId}>
              <TableCell>{metric.sensorId}</TableCell>
              <TableCell align="right">{metric.avgTemperature.toFixed(2)}</TableCell>
              <TableCell align="right">{metric.avgHumidity.toFixed(2)}</TableCell>
              <TableCell align="right">{metric.minTemperature.toFixed(2)}</TableCell>
              <TableCell align="right">{metric.maxTemperature.toFixed(2)}</TableCell>
              <TableCell align="right">{metric.minHumidity.toFixed(2)}</TableCell>
              <TableCell align="right">{metric.maxHumidity.toFixed(2)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

export default MetricsTable;
