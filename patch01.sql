DROP TABLE IF EXISTS HR_DATES;
CREATE TABLE HR_DATES (
    ID MEDIUMINT NOT NULL AUTO_INCREMENT,
    YEAR INT NOT NULL,
    MONTH INT NOT NULL,
    REG_DATE DATE NOT NULL,
    UNIQUE (YEAR, MONTH),
    PRIMARY KEY (ID))
  ENGINE=MyISAM DEFAULT CHARSET=utf8;

INSERT INTO HR_DATES (ID, YEAR, MONTH, REG_DATE) VALUES (DEFAULT, 2012, 5, '2012-06-26');

