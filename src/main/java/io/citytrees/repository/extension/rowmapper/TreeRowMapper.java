package io.citytrees.repository.extension.rowmapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.citytrees.model.Tree;
import io.citytrees.service.GeometryService;
import io.citytrees.v1.model.TreeCondition;
import io.citytrees.v1.model.TreeState;
import io.citytrees.v1.model.TreeStatus;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TreeRowMapper implements RowMapper<Tree> {

    private final GeometryService geometryService;
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public Tree mapRow(ResultSet rs, int rowNum) {
        Tree.TreeBuilder builder = Tree.builder();
        PGobject geoPoint = rs.getObject("geo_point", PGobject.class);
        Point point = (Point) PGgeometry.geomFromString(Objects.requireNonNull(geoPoint.getValue()));
        String state = rs.getObject("state", String.class);
        String condition = rs.getObject("condition", String.class);
        String barkCondition = rs.getString("bark_condition");
        String branchesCondition = rs.getString("branches_condition");

        return builder
            .id(rs.getObject("id", UUID.class))
            .userId(rs.getObject("user_id", UUID.class))
            .status(TreeStatus.valueOf(rs.getObject("status", String.class)))
            .geoPoint(geometryService.createPoint(point.getX(), point.getY()))
            .fileIds(objectMapper.readValue(rs.getString("file_ids"), new TypeReference<>() {}))
            .state(state != null ? TreeState.valueOf(state) : null)
            .condition(condition != null ? TreeCondition.valueOf(condition) : null)
            .barkCondition(barkCondition != null ? objectMapper.readValue(barkCondition, new TypeReference<>() {}) : null)
            .branchesCondition(branchesCondition != null ? objectMapper.readValue(branchesCondition, new TypeReference<>() {}) : null)
            .comment(rs.getString("comment"))
            .build();
    }
}
