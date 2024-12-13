import React from "react";
import { Button } from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";

const RefreshButton = ({ onRefresh }) => {
  return (
    <Button
      variant="contained"
      color="primary"
      startIcon={<RefreshIcon />}
      onClick={onRefresh}
      style={{ marginTop: "10px" }}
    >
      Refresh Data
    </Button>
  );
};

export default RefreshButton;
