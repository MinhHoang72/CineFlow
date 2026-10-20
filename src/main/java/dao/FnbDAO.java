package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import model.entity.ComboLine;
import model.entity.FnbCombo;
import model.entity.FnbItem;
import model.dto.FnbRowDTO;
import model.dto.OptionDTO;
import model.dto.PageDTO;
import model.dto.StockRowDTO;
import utils.DBContext;

public class FnbDAO {

    private static final String MENU
            = "SELECT 'ITEM' AS Kind, i.FnbItemID AS Id, i.ItemName AS Name, c.CategoryName AS CategoryName, "
            + "i.CategoryID AS CategoryID, i.Price AS Price, i.Status AS Status, i.ImageURL AS ImageURL, "
            + "i.Description AS Description, i.CreatedAt AS CreatedAt, i.CentreID AS CentreID, ct.CentreName AS CentreName "
            + "FROM FnbItems i JOIN FnbCategories c ON c.CategoryID = i.CategoryID "
            + "JOIN Centres ct ON ct.CentreID = i.CentreID "
            + "UNION ALL "
            + "SELECT 'COMBO', co.ComboID, co.ComboName, N'Combo', NULL, co.Price, co.Status, co.ImageURL, co.Description, "
            + "co.CreatedAt, co.CentreID, ct2.CentreName "
            + "FROM FnbCombos co JOIN Centres ct2 ON ct2.CentreID = co.CentreID";

    public List<OptionDTO> categories() {
        return options("SELECT CategoryID, CategoryName FROM FnbCategories WHERE Status = 'ACTIVE' ORDER BY CategoryName");
    }

    public List<OptionDTO> centres() {
        return options("SELECT CentreID, CentreName FROM Centres ORDER BY CentreName");
    }

    public PageDTO<FnbRowDTO> search(String q, String category, String status, BigDecimal minPrice,
            BigDecimal maxPrice, String sort, Integer centreId, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (centreId != null) {
            where.append(" AND x.CentreID = ?");
            params.add(centreId);
        }
        if (q != null && !q.isEmpty()) {
            where.append(" AND (x.Name COLLATE Latin1_General_100_CI_AI LIKE ?"
                    + " OR x.Description COLLATE Latin1_General_100_CI_AI LIKE ?)");
            String like = "%" + q.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]") + "%";
            params.add(like);
            params.add(like);
        }
        if (minPrice != null) {
            where.append(" AND x.Price >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            where.append(" AND x.Price <= ?");
            params.add(maxPrice);
        }
        if ("COMBO".equals(category)) {
            where.append(" AND x.Kind = 'COMBO'");
        } else if (category != null && !category.isEmpty()) {
            where.append(" AND x.CategoryID = ?");
            params.add(Integer.valueOf(category));
        }
        if (status != null && !status.isEmpty()) {
            where.append(" AND x.Status = ?");
            params.add(status);
        }

        String orderBy;
        switch (sort == null ? "" : sort) {
            case "name":
                orderBy = "x.Name";
                break;
            case "price_asc":
                orderBy = "x.Price, x.Name";
                break;
            case "price_desc":
                orderBy = "x.Price DESC, x.Name";
                break;
            case "newest":
                orderBy = "x.CreatedAt DESC";
                break;
            default:
                orderBy = "x.Kind, x.Id";
        }

        String from = " FROM (" + MENU + ") x" + where;
        List<FnbRowDTO> rows = new ArrayList<>();
        int total = 0;

        try (Connection connection = DBContext.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*)" + from)) {
                for (int i = 0; i < params.size(); i++) {
                    statement.setObject(i + 1, params.get(i));
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    total = resultSet.getInt(1);
                }
            }

            List<Object> pageParams = new ArrayList<>(params);
            pageParams.add((page - 1) * size);
            pageParams.add(size);
            String sql = "SELECT x.Kind, x.Id, x.Name, x.CategoryName, x.Price, x.Status, x.ImageURL, "
                    + "x.CentreID, x.CentreName" + from
                    + " ORDER BY " + orderBy + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < pageParams.size(); i++) {
                    statement.setObject(i + 1, pageParams.get(i));
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        FnbRowDTO row = new FnbRowDTO();
                        row.setKind(resultSet.getString("Kind"));
                        row.setId(resultSet.getInt("Id"));
                        row.setName(resultSet.getString("Name"));
                        row.setCategoryName(resultSet.getString("CategoryName"));
                        row.setPrice(resultSet.getBigDecimal("Price"));
                        row.setStatus(resultSet.getString("Status"));
                        row.setImageUrl(resultSet.getString("ImageURL"));
                        row.setCentreId(resultSet.getInt("CentreID"));
                        row.setCentreName(resultSet.getString("CentreName"));
                        rows.add(row);
                    }
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return new PageDTO<>(rows, page, size, total);
    }

    public FnbItem findItem(int fnbItemId) {
        String sql = "SELECT FnbItemID, CategoryID, ItemName, Description, Price, ImageURL, Status, CentreID "
                + "FROM FnbItems WHERE FnbItemID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, fnbItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    FnbItem item = new FnbItem();
                    item.setFnbItemId(resultSet.getInt("FnbItemID"));
                    item.setCategoryId(resultSet.getInt("CategoryID"));
                    item.setItemName(resultSet.getString("ItemName"));
                    item.setDescription(resultSet.getString("Description"));
                    item.setPrice(resultSet.getBigDecimal("Price"));
                    item.setImageUrl(resultSet.getString("ImageURL"));
                    item.setStatus(resultSet.getString("Status"));
                    item.setCentreId(resultSet.getInt("CentreID"));
                    return item;
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public List<FnbItem> activeItems(int centreId) {
        String sql = "SELECT FnbItemID, CategoryID, ItemName, Description, Price, ImageURL, Status, CentreID "
                + "FROM FnbItems WHERE Status = 'ACTIVE' AND CentreID = ? ORDER BY ItemName";
        List<FnbItem> items = new ArrayList<>();
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, centreId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    FnbItem item = new FnbItem();
                    item.setFnbItemId(resultSet.getInt("FnbItemID"));
                    item.setCategoryId(resultSet.getInt("CategoryID"));
                    item.setItemName(resultSet.getString("ItemName"));
                    item.setDescription(resultSet.getString("Description"));
                    item.setPrice(resultSet.getBigDecimal("Price"));
                    item.setImageUrl(resultSet.getString("ImageURL"));
                    item.setStatus(resultSet.getString("Status"));
                    item.setCentreId(resultSet.getInt("CentreID"));
                    items.add(item);
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return items;
    }

    public void insertItem(FnbItem item) throws SQLException {
        String sql = "INSERT INTO FnbItems (CategoryID, ItemName, Description, Price, ImageURL, Status, CentreID) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, item.getCategoryId());
            statement.setString(2, item.getItemName());
            statement.setString(3, item.getDescription());
            statement.setBigDecimal(4, item.getPrice());
            statement.setString(5, item.getImageUrl());
            statement.setString(6, item.getStatus());
            statement.setInt(7, item.getCentreId());
            statement.executeUpdate();
        }
    }

    public void updateItem(FnbItem item) throws SQLException {
        String sql = "UPDATE FnbItems SET CategoryID = ?, ItemName = ?, Description = ?, Price = ?, "
                + "ImageURL = ?, Status = ?, UpdatedAt = SYSDATETIME() WHERE FnbItemID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, item.getCategoryId());
            statement.setString(2, item.getItemName());
            statement.setString(3, item.getDescription());
            statement.setBigDecimal(4, item.getPrice());
            statement.setString(5, item.getImageUrl());
            statement.setString(6, item.getStatus());
            statement.setInt(7, item.getFnbItemId());
            statement.executeUpdate();
        }
    }

    public int countActiveCombosUsing(int fnbItemId) {
        String sql = "SELECT COUNT(DISTINCT co.ComboID) FROM ComboItems ci "
                + "JOIN FnbCombos co ON co.ComboID = ci.ComboID "
                + "WHERE ci.FnbItemID = ? AND co.Status = 'ACTIVE'";
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, fnbItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    public FnbCombo findCombo(int comboId) {
        String comboSql = "SELECT ComboID, ComboName, Description, Price, ImageURL, Status, CentreID "
                + "FROM FnbCombos WHERE ComboID = ?";
        String linesSql = "SELECT FnbItemID, Quantity FROM ComboItems WHERE ComboID = ? ORDER BY ComboItemID";
        try (Connection connection = DBContext.getConnection()) {
            FnbCombo combo = new FnbCombo();
            try (PreparedStatement statement = connection.prepareStatement(comboSql)) {
                statement.setInt(1, comboId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    combo.setComboId(resultSet.getInt("ComboID"));
                    combo.setComboName(resultSet.getString("ComboName"));
                    combo.setDescription(resultSet.getString("Description"));
                    combo.setPrice(resultSet.getBigDecimal("Price"));
                    combo.setImageUrl(resultSet.getString("ImageURL"));
                    combo.setStatus(resultSet.getString("Status"));
                    combo.setCentreId(resultSet.getInt("CentreID"));
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(linesSql)) {
                statement.setInt(1, comboId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        combo.getLines().add(new ComboLine(resultSet.getInt("FnbItemID"), resultSet.getInt("Quantity")));
                    }
                }
            }
            return combo;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public void saveCombo(FnbCombo combo) throws SQLException {
        try (Connection connection = DBContext.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int comboId = combo.getComboId();
                if (comboId == 0) {
                    String insertSql = "INSERT INTO FnbCombos (ComboName, Description, Price, ImageURL, Status, CentreID) "
                            + "VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement statement
                            = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                        statement.setString(1, combo.getComboName());
                        statement.setString(2, combo.getDescription());
                        statement.setBigDecimal(3, combo.getPrice());
                        statement.setString(4, combo.getImageUrl());
                        statement.setString(5, combo.getStatus());
                        statement.setInt(6, combo.getCentreId());
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            keys.next();
                            comboId = keys.getInt(1);
                        }
                    }
                } else {
                    String updateSql = "UPDATE FnbCombos SET ComboName = ?, Description = ?, Price = ?, "
                            + "ImageURL = ?, Status = ?, UpdatedAt = SYSDATETIME() WHERE ComboID = ?";
                    try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                        statement.setString(1, combo.getComboName());
                        statement.setString(2, combo.getDescription());
                        statement.setBigDecimal(3, combo.getPrice());
                        statement.setString(4, combo.getImageUrl());
                        statement.setString(5, combo.getStatus());
                        statement.setInt(6, comboId);
                        statement.executeUpdate();
                    }
                    try (PreparedStatement statement
                            = connection.prepareStatement("DELETE FROM ComboItems WHERE ComboID = ?")) {
                        statement.setInt(1, comboId);
                        statement.executeUpdate();
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO ComboItems (ComboID, FnbItemID, Quantity) VALUES (?, ?, ?)")) {
                    for (ComboLine line : combo.getLines()) {
                        statement.setInt(1, comboId);
                        statement.setInt(2, line.getFnbItemId());
                        statement.setInt(3, line.getQuantity());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public boolean setStatus(boolean combo, int id, String status) throws SQLException {
        String sql = combo
                ? "UPDATE FnbCombos SET Status = ?, UpdatedAt = SYSDATETIME() WHERE ComboID = ?"
                : "UPDATE FnbItems SET Status = ?, UpdatedAt = SYSDATETIME() WHERE FnbItemID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, id);
            return statement.executeUpdate() > 0;
        }
    }

    public List<StockRowDTO> stock(int centreId) {
        String sql = "SELECT i.FnbItemID, i.ItemName, cat.CategoryName, "
                + "CAST(ISNULL(inv.IsAvailable, 0) AS bit) AS IsAvailable "
                + "FROM FnbItems i "
                + "JOIN FnbCategories cat ON cat.CategoryID = i.CategoryID "
                + "LEFT JOIN CentreFnbInventory inv ON inv.FnbItemID = i.FnbItemID AND inv.CentreID = ? "
                + "WHERE i.Status = 'ACTIVE' AND i.CentreID = ? "
                + "ORDER BY cat.CategoryName, i.ItemName";
        List<StockRowDTO> rows = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, centreId);
            statement.setInt(2, centreId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    StockRowDTO row = new StockRowDTO();
                    row.setFnbItemId(resultSet.getInt("FnbItemID"));
                    row.setItemName(resultSet.getString("ItemName"));
                    row.setCategoryName(resultSet.getString("CategoryName"));
                    row.setAvailable(resultSet.getBoolean("IsAvailable"));
                    rows.add(row);
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return rows;
    }

    public boolean isStockableItem(int fnbItemId, int centreId) {
        String sql = "SELECT COUNT(*) FROM FnbItems WHERE FnbItemID = ? AND Status = 'ACTIVE' AND CentreID = ?";
        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, fnbItemId);
            statement.setInt(2, centreId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public void setAvailability(int centreId, int fnbItemId, boolean available) throws SQLException {
        try (Connection connection = DBContext.getConnection()) {
            String updateSql = "UPDATE CentreFnbInventory SET IsAvailable = ?, UpdatedAt = SYSDATETIME() "
                    + "WHERE CentreID = ? AND FnbItemID = ?";
            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setBoolean(1, available);
                statement.setInt(2, centreId);
                statement.setInt(3, fnbItemId);
                if (statement.executeUpdate() > 0) {
                    return;
                }
            }
            String insertSql = "INSERT INTO CentreFnbInventory (CentreID, FnbItemID, IsAvailable) VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                statement.setInt(1, centreId);
                statement.setInt(2, fnbItemId);
                statement.setBoolean(3, available);
                statement.executeUpdate();
            }
        }
    }

    public String centreLabel(Integer centreId) {
        if (centreId == null) {
            return null;
        }
        String sql = "SELECT CentreName FROM Centres WHERE CentreID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, centreId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return "Centre " + centreId + " - " + resultSet.getString(1);
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    private List<OptionDTO> options(String sql) {
        List<OptionDTO> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                list.add(new OptionDTO(resultSet.getInt(1), resultSet.getString(2)));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return list;
    }
}
