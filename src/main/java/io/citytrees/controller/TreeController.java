package io.citytrees.controller;

import io.citytrees.model.CtFile;
import io.citytrees.util.FileDownloadUtil;
import io.citytrees.service.TreeService;
import io.citytrees.v1.controller.TreeControllerApiDelegate;
import io.citytrees.v1.model.FileUploadResponse;
import io.citytrees.v1.model.TreeCountAllGetResponse;
import io.citytrees.v1.model.TreeCreateRequest;
import io.citytrees.v1.model.TreeCreateResponse;
import io.citytrees.v1.model.TreeGetAttachedFileResponse;
import io.citytrees.v1.model.TreeGetResponse;
import io.citytrees.v1.model.TreeStatus;
import io.citytrees.v1.model.TreeUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TreeController implements TreeControllerApiDelegate {

    private final TreeService treeService;
    private final FileDownloadUtil fileDownloadUtil;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TreeCreateResponse> createTree(TreeCreateRequest treeCreateRequest) {
        Long treeId = treeService.create(treeCreateRequest);
        TreeCreateResponse response = new TreeCreateResponse()
            .treeId(treeId);
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<TreeGetResponse> getTreeById(Long id) {
        var optionalTree = treeService.getById(id);
        if (optionalTree.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        var response = treeService.responseFromTree(optionalTree.get());
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasAuthority(@Roles.ADMIN) || (isAuthenticated() && hasPermission(#id, @Domains.TREE, @Permissions.EDIT))")
    public ResponseEntity<Void> updateTreeById(Long id, TreeUpdateRequest treeUpdateRequest) {
        treeService.update(id, treeUpdateRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasAuthority(@Roles.ADMIN) || (isAuthenticated() && hasPermission(#id, @Domains.TREE, @Permissions.DELETE))")
    public ResponseEntity<Void> deleteTree(Long id) {
        treeService.delete(id);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasAnyAuthority(@Roles.ADMIN, @Roles.MODERATOR)")
    public ResponseEntity<Void> approveTree(Long treeId) {
        treeService.updateStatus(treeId, TreeStatus.APPROVED);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileUploadResponse> attachFile(Long treeId, MultipartFile file) {
        var fileId = treeService.attachFile(treeId, file);
        var response = new FileUploadResponse()
            .fileId(fileId)
            .url(fileDownloadUtil.generateDownloadUrl(fileId));

        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<TreeGetAttachedFileResponse>> getAllAttachedFiles(Long treeId) {
        List<CtFile> files = treeService.listAttachedFiles(treeId);

        List<TreeGetAttachedFileResponse> response = files.stream()
            .map(file -> new TreeGetAttachedFileResponse()
                .id(file.getId())
                .name(file.getName())
                .size(file.getSize())
                .url(fileDownloadUtil.generateDownloadUrl(file.getId())))
            .toList();

        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<TreeGetResponse>> getAll(Integer limit, Integer offset) {
        var response = treeService.listAll(limit, offset).stream()
            .map(treeService::responseFromTree)
            .toList();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TreeCountAllGetResponse> getAllTreesCount() {
        return ResponseEntity.ok(new TreeCountAllGetResponse().count(treeService.countAll()));
    }
}
