CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS "User" (
    "UserId" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "GoogleSub" VARCHAR(100) NOT NULL UNIQUE,
    "Email" VARCHAR(256) NOT NULL,
    "FullName" VARCHAR(150),
    "Gender" VARCHAR(10),
    "Age" INTEGER,
    "Role" VARCHAR(10) NOT NULL DEFAULT 'USER',
    "CreatedAt" TIMESTAMP NOT NULL,
    "UpdatedAt" TIMESTAMP,
    CONSTRAINT "CK_User_Role" CHECK ("Role" IN ('USER', 'ADMIN')),
    CONSTRAINT "CK_User_Age" CHECK ("Age" IS NULL OR ("Age" >= 0 AND "Age" <= 120))
);

CREATE TABLE IF NOT EXISTS "ImageUpload" (
    "ImageId" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "UserId" UUID NOT NULL,
    "FileUri" VARCHAR(500) NOT NULL,
    "FileHashSha256" VARCHAR(64),
    "FileSizeBytes" BIGINT,
    "UploadedAt" TIMESTAMP NOT NULL,
    CONSTRAINT "FK_ImageUpload_User"
        FOREIGN KEY ("UserId")
        REFERENCES "User" ("UserId")
        ON DELETE CASCADE,
    CONSTRAINT "CK_ImageUpload_FileSizeBytes" CHECK ("FileSizeBytes" IS NULL OR "FileSizeBytes" >= 0)
);

CREATE TABLE IF NOT EXISTS "Prediction" (
    "PredictionId" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "ImageId" UUID NOT NULL,
    "RequestedAt" TIMESTAMP NOT NULL,
    "ClientApp" VARCHAR(50),
    "ClientIp" VARCHAR(50),
    "ModelName" VARCHAR(100),
    "ModelVersion" VARCHAR(50),
    "PredictedClass" VARCHAR(100) NOT NULL,
    "Probability" NUMERIC(6,5) NOT NULL,
    "TopKJson" TEXT,
    "RawResponseJson" TEXT,
    CONSTRAINT "FK_Prediction_ImageUpload"
        FOREIGN KEY ("ImageId")
        REFERENCES "ImageUpload" ("ImageId")
        ON DELETE CASCADE,
    CONSTRAINT "CK_Prediction_Probability" CHECK ("Probability" >= 0 AND "Probability" <= 1)
);

CREATE TABLE IF NOT EXISTS "Feedback" (
    "FeedbackId" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "PredictionId" UUID NOT NULL,
    "UserId" UUID NOT NULL,
    "IsCorrect" BOOLEAN,
    "UserLabel" VARCHAR(100),
    "Comment" VARCHAR(1000),
    "AllowForRetrain" BOOLEAN NOT NULL DEFAULT FALSE,
    "CreatedAt" TIMESTAMP NOT NULL,
    CONSTRAINT "FK_Feedback_Prediction"
        FOREIGN KEY ("PredictionId")
        REFERENCES "Prediction" ("PredictionId")
        ON DELETE CASCADE,
    CONSTRAINT "FK_Feedback_User"
        FOREIGN KEY ("UserId")
        REFERENCES "User" ("UserId")
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS "IX_User_Email" ON "User" ("Email");
CREATE INDEX IF NOT EXISTS "IX_ImageUpload_UserId_UploadedAt" ON "ImageUpload" ("UserId", "UploadedAt" DESC);
CREATE INDEX IF NOT EXISTS "IX_Prediction_ImageId" ON "Prediction" ("ImageId");
CREATE INDEX IF NOT EXISTS "IX_Prediction_RequestedAt" ON "Prediction" ("RequestedAt" DESC);
CREATE INDEX IF NOT EXISTS "IX_Feedback_PredictionId_CreatedAt" ON "Feedback" ("PredictionId", "CreatedAt" DESC);
CREATE INDEX IF NOT EXISTS "IX_Feedback_UserId_CreatedAt" ON "Feedback" ("UserId", "CreatedAt" DESC);
CREATE INDEX IF NOT EXISTS "IX_Feedback_AllowForRetrain_CreatedAt" ON "Feedback" ("AllowForRetrain", "CreatedAt" DESC);
