# Start with an official OpenJDK image
FROM openjdk:17-jdk-slim

# Install sbt
RUN apt-get update && apt-get install -y curl gnupg && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" > /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x99e82a75642ac823" | apt-key add && \
    apt-get update && apt-get install -y sbt

# Set the working directory
WORKDIR /app

# Copy the project files
COPY . .

# Pre-fetch dependencies to speed up builds
RUN sbt update

# Expose the application port (if necessary, e.g., for HTTP servers)
EXPOSE 9000

# Run the application
CMD ["sbt", "run"]