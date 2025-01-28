/* src/test/resources/schema.sql */
CREATE ALIAS IF NOT EXISTS SPATIAL_INIT FOR "org.h2gis.functions.factory.H2GISFunctions.load";
CALL SPATIAL_INIT();