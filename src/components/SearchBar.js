import React from "react";
import { TextField } from "@mui/material";

const SearchBar = ({ searchTerm, onSearch }) => {
  return (
    <TextField
      label="Search by Sensor ID"
      variant="outlined"
      fullWidth
      margin="normal"
      value={searchTerm}
      onChange={(e) => onSearch(e.target.value)}
    />
  );
};

export default SearchBar;
