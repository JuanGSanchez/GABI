# GABI
Java training software, command-line database management for libraries.  
Small open code for the management of books, members and loans with a database.  
Use of DAO pattern for Derby database,
connections via singleton classes, applicable to other databases.

To build up the database, in the *configuration.properties* file you can change the names
and properties of the database tables' properties. Set **database-isbuilt** property to **false**,
and run the program. The main class will automatically trigger the creation of the database from
scratch, and set this property to **true** when finished.

When building up the database for the first time, Derby must be reset in order to apply
correctly the authentication settings. This can be done while the program is running and
no connection with the database is established.

It is possible to reset the database, cleaning all the tables' entries, just setting again
**database-isbuilt** property to **false** before running again the program.

This program has localization functionality fully implemented, thanks to the *statements.properties*
resource bundle.

The program could be scaled to support more entities and their corresponding managers, following
the abstract classes and interfaces located in the packages.