-- conf/evolutions/default/1.sql
# --- !Ups
CREATE TABLE person (
                        sno SERIAL PRIMARY KEY,
                        name VARCHAR(255),
                        city VARCHAR(255)
);

# --- !Downs
DROP TABLE IF EXISTS person;
