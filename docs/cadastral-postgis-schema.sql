-- UrbanGrid cadastral production schema (PostgreSQL 15+ with PostGIS 3+).
-- Separate from the existing MongoDB chat service, so it can be applied by a
-- dedicated GIS worker without changing current chat routes.
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE cadastral_dataset_kind AS ENUM ('ORTHOMOSAIC', 'DSM', 'DTM', 'PARCELS', 'BUILDINGS', 'ROADS', 'GROUND_TRUTH');
CREATE TYPE cadastral_job_status AS ENUM ('QUEUED', 'VALIDATING', 'PROCESSING', 'COMPLETED', 'FAILED');
CREATE TYPE cadastral_parcel_status AS ENUM ('MATCHED', 'BOUNDARY_MISMATCH', 'OVERLAPPING', 'INVALID', 'REQUIRES_VERIFICATION', 'VERIFIED');
CREATE TYPE cadastral_verification_status AS ENUM ('UNVERIFIED', 'VERIFIED', 'CORRECTED', 'REJECTED', 'REQUIRES_RESURVEY');

CREATE TABLE cadastral_projects (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name text NOT NULL, description text,
  srid integer NOT NULL DEFAULT 4326, boundary geometry(MultiPolygon, 4326),
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX cadastral_projects_boundary_gix ON cadastral_projects USING gist (boundary);

CREATE TABLE cadastral_datasets (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), project_id uuid NOT NULL REFERENCES cadastral_projects(id) ON DELETE CASCADE,
  kind cadastral_dataset_kind NOT NULL, source_uri text NOT NULL, original_filename text NOT NULL, crs text,
  bounds geometry(Polygon, 4326), metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  processing_status cadastral_job_status NOT NULL DEFAULT 'QUEUED', created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX cadastral_datasets_project_idx ON cadastral_datasets(project_id);
CREATE INDEX cadastral_datasets_bounds_gix ON cadastral_datasets USING gist (bounds);

CREATE TABLE cadastral_parcels (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), project_id uuid NOT NULL REFERENCES cadastral_projects(id) ON DELETE CASCADE,
  parcel_number text NOT NULL, geometry geometry(Polygon, 4326) NOT NULL, area_sq_m numeric(14,2) NOT NULL,
  perimeter_m numeric(14,2) NOT NULL, ai_confidence numeric(5,2), land_use text,
  registry_attributes jsonb NOT NULL DEFAULT '{}'::jsonb,
  status cadastral_parcel_status NOT NULL DEFAULT 'REQUIRES_VERIFICATION',
  verification_status cadastral_verification_status NOT NULL DEFAULT 'UNVERIFIED',
  is_ai_generated boolean NOT NULL DEFAULT true, revision integer NOT NULL DEFAULT 1,
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(project_id, parcel_number)
);
CREATE INDEX cadastral_parcels_project_idx ON cadastral_parcels(project_id);
CREATE INDEX cadastral_parcels_geometry_gix ON cadastral_parcels USING gist (geometry);

CREATE TABLE cadastral_buildings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), project_id uuid NOT NULL REFERENCES cadastral_projects(id) ON DELETE CASCADE,
  geometry geometry(Polygon, 4326) NOT NULL, ai_confidence numeric(5,2), attributes jsonb NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX cadastral_buildings_geometry_gix ON cadastral_buildings USING gist (geometry);

CREATE TABLE cadastral_roads (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), project_id uuid NOT NULL REFERENCES cadastral_projects(id) ON DELETE CASCADE,
  geometry geometry(LineString, 4326) NOT NULL, ai_confidence numeric(5,2), road_class text, attributes jsonb NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX cadastral_roads_geometry_gix ON cadastral_roads USING gist (geometry);

CREATE TABLE cadastral_ground_truth (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), project_id uuid NOT NULL REFERENCES cadastral_projects(id) ON DELETE CASCADE,
  parcel_id uuid REFERENCES cadastral_parcels(id) ON DELETE SET NULL, geometry geometry(Geometry, 4326) NOT NULL,
  status cadastral_verification_status NOT NULL DEFAULT 'UNVERIFIED', comments text, surveyor_id text,
  observed_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX cadastral_ground_truth_geometry_gix ON cadastral_ground_truth USING gist (geometry);

CREATE TABLE cadastral_validation_issues (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), project_id uuid NOT NULL REFERENCES cadastral_projects(id) ON DELETE CASCADE,
  parcel_id uuid REFERENCES cadastral_parcels(id) ON DELETE SET NULL,
  issue_type text NOT NULL CHECK (issue_type IN ('OVERLAP', 'GAP', 'INVALID_GEOMETRY', 'DUPLICATE', 'BUILDING_CONFLICT', 'ROAD_CONFLICT', 'BOUNDARY_MISMATCH')),
  severity text NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'ERROR')), geometry geometry(Geometry, 4326),
  details jsonb NOT NULL DEFAULT '{}'::jsonb, resolved_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX cadastral_validation_issues_geometry_gix ON cadastral_validation_issues USING gist (geometry);
CREATE INDEX cadastral_validation_issues_open_idx ON cadastral_validation_issues(project_id) WHERE resolved_at IS NULL;
