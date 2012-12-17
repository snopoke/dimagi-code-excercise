CREATE TABLE whereami.people
    (id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100),
    email VARCHAR(50),
    PRIMARY KEY (id),
    UNIQUE (email));

CREATE TABLE whereami.locations
    (id INT NOT NULL AUTO_INCREMENT,
    person_id INT,
    date TIMESTAMP,
    location VARCHAR(50),
    lat FLOAT,
    lng FLOAT,
    PRIMARY KEY (id));

ALTER TABLE whereami.locations
    ADD FOREIGN KEY (person_id)
    REFERENCES whereami.people(id);