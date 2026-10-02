package com.mysociety.identity.repository;

import com.mysociety.identity.domain.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Object> {
    @Query(value = "select p.code from mysociety.role_permissions rp join mysociety.permissions p on p.id=rp.permission_id where rp.role_id in :roleIds", nativeQuery = true)
    List<String> permissionCodes(@Param("roleIds") Collection<UUID> roleIds);
}
