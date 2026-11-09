package com.campus.remarket.controller;
import com.campus.remarket.service.*;
import com.campus.remarket.portal.StaffAccess;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.core.io.Resource;
@RestController public class CatalogController {
 private final StaffAccess access;private final SchoolCatalogService schools;private final BusinessCategoryService categories;private final SchoolImages images;private final CampusScope scope;
 public CatalogController(StaffAccess access,SchoolCatalogService schools,BusinessCategoryService categories,SchoolImages images,CampusScope scope){this.access=access;this.schools=schools;this.categories=categories;this.images=images;this.scope=scope;}
 @GetMapping("/api/campus") public Map<String,Object> campus(){var s=scope.school();var m=new LinkedHashMap<String,Object>();m.put("id",s.getId());m.put("name",s.getName());m.put("address",s.getAddress());m.put("latitude",s.getLatitude());m.put("longitude",s.getLongitude());m.put("photo",s.getPhoto());m.put("mapImage",s.getMapImage());return m;}
 @GetMapping("/api/business-categories") public List<BusinessCategoryService.Category> publicCategories(){return categories.list();}
 @GetMapping("/api/admin/catalog/schools") public List<SchoolCatalogService.View> schools(){access.admin();return schools.list();}
 @PostMapping("/api/admin/catalog/schools") public SchoolCatalogService.View addSchool(@RequestBody SchoolCatalogService.Input in,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){access.admin();access.verify(csrf);return schools.save(null,in);}
 @PutMapping("/api/admin/catalog/schools/{id}") public SchoolCatalogService.View school(@PathVariable Long id,@RequestBody SchoolCatalogService.Input in,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){access.admin();access.verify(csrf);return schools.save(id,in);}
 @DeleteMapping("/api/admin/catalog/schools/{id}") public void deleteSchool(@PathVariable Long id,@RequestParam Long version,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){access.admin();access.verify(csrf);schools.delete(id,version);}
 public record CategoryInput(String name,Long version){}
 @GetMapping("/api/admin/catalog/categories") public List<BusinessCategoryService.Category> categories(){access.admin();return categories.list();}
 @PostMapping("/api/admin/catalog/categories") public void addCategory(@RequestBody CategoryInput in,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){access.admin();access.verify(csrf);categories.save(null,in.name(),null);}
 @PutMapping("/api/admin/catalog/categories/{id}") public void category(@PathVariable Long id,@RequestBody CategoryInput in,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){access.admin();access.verify(csrf);categories.save(id,in.name(),in.version());}
 @DeleteMapping("/api/admin/catalog/categories/{id}") public void deleteCategory(@PathVariable Long id,@RequestParam Long version,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){access.admin();access.verify(csrf);categories.delete(id,version);}
 @PostMapping("/api/admin/catalog/images") public Map<String,String> upload(@RequestParam MultipartFile file,@RequestHeader(value="X-CSRF-Token",required=false)String csrf)throws java.io.IOException{access.admin();access.verify(csrf);return Map.of("name",images.save(file));}
 @GetMapping("/api/school-images/{name}") public ResponseEntity<Resource> image(@PathVariable String name){var r=images.load(name);return r==null?ResponseEntity.notFound().build():ResponseEntity.ok().contentType(name.endsWith(".png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG).header("X-Content-Type-Options","nosniff").body(r);}
}
