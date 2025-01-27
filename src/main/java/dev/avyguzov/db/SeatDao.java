package dev.avyguzov.db;

import dev.avyguzov.model.Seat;
import org.apache.logging.log4j.LogManager;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SeatDao {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(SeatDao.class);
    private final Database database;

    @Inject
    public SeatDao(Database database) {
        this.database = database;
    }

    public List<Seat> getAllSeats() throws SQLException {
        String getAllSeatsSql = "SELECT * FROM seats;";

        Connection conn = database.getConnection();
        Statement statement = conn.createStatement();
        ResultSet rs = statement.executeQuery(getAllSeatsSql);

        logger.info("Successfully get all seats from DB");

        var seats = new ArrayList<Seat>();

        while (rs.next()) {
            Seat seat = new Seat();
            seat.setId(rs.getInt("id"));
            seat.setOccupied(rs.getBoolean("is_occupied"));
            seat.setVersion(rs.getInt("version"));
            seats.add(seat);
        }

        statement.close();
        conn.close();

        return seats;
    }

    /*
     * seatsIds seat that user wants to occupy.
     * @return boolean. Was seats have been occupied or not.
     */
    public boolean takeSeats(List<Integer> seatsIds) throws SQLException {
        if (seatsIds.size() == 0) {
            String errorMessage = "Must be specified at least one seat";
            LogManager.getLogger("errors").error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        try (Connection conn = database.getConnection();
             Statement statement = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = getAllAvailableSeatsRs(statement, seatsIds)) {

            int rowCount = rs.last() ? rs.getRow() : 0;
            if (rowCount != seatsIds.size()) {
                logger.info("One or more of required seats are taken before try");
                return false;
            }
            rs.beforeFirst();
            if (takeAllSeatsFromRs(conn, rs)) {
                conn.commit();
                logger.info("All seats are occupied successfully");
            } else {
                conn.rollback();
                logger.info("Somebody has taken seat while trying to occupy seats");
                return false;
            }
        }

        return true;
    }
    
    private ResultSet getAllAvailableSeatsRs(Statement statement, List<Integer> seatsIds) throws SQLException {
        return statement.executeQuery("SELECT * FROM seats WHERE id IN (" +
                String.join(", ", String.join(", ", convertList(seatsIds))) +
                ") AND is_occupied=false;");
    }

    /*
     * @return boolean. All updates was successful?
     */
    private boolean takeAllSeatsFromRs(Connection conn, ResultSet rs) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            while (rs.next()) {
                StringBuilder takeSeatsSql = new StringBuilder();
                int seatVersion = rs.getInt("version");
                int seatId = rs.getInt("id");

                takeSeatsSql.append("UPDATE seats SET is_occupied = true, version=");
                takeSeatsSql.append(seatVersion + 1);
                takeSeatsSql.append(" ");
                takeSeatsSql.append("WHERE id=");
                takeSeatsSql.append(seatId);
                takeSeatsSql.append(" AND version=");
                takeSeatsSql.append(seatVersion);
                takeSeatsSql.append(";\n");

                if (statement.executeUpdate(takeSeatsSql.toString()) != 1) {
                    return false;
                }
            }            
        }
        return true;
    }

    private <T> List<String> convertList(List<T> list) {
        return list.stream().map(String::valueOf).collect(Collectors.toList());
    }
}
