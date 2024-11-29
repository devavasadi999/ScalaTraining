### For Generating DESC file
protoc --descriptor_set_out=/Users/vinodh/protos/Employee.desc --include_imports Employee.proto

### For Generating SCALA Codegit
protoc --scala_out=src/main/scala --proto_path=src/main/scala/example/protos src/main/scala/example/protos/Employee.proto
