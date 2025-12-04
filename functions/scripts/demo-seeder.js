'use strict';

/**
 * Demo data seeder.
 *
 * Creates:
 * - One teacher account (email/password) with a classroom + topics.
 * - Five student accounts (email/password) enrolled in that classroom.
 * - Inline learning materials based on Q2 LAS content (no docx scanning).
 * - Topic-specific quizzes (one quiz per LAS topic).
 * - Classroom-wide pre- and post-tests (not tied to a single topic).
 *
 * Requirements:
 * - Service account credentials (GOOGLE_APPLICATION_CREDENTIALS) or emulator env.
 * - Run from repo root: `npm --prefix functions run seed:demo`
 * - Optional env overrides:
 *   SEED_PASSWORD, SEED_TEACHER_EMAIL, SEED_TEACHER_NAME,
 *   SEED_STUDENT_EMAILS (comma-separated),
 *   SEED_CLASSROOM_ID, SEED_CLASSROOM_NAME, SEED_JOIN_CODE,
 *   SEED_TOPIC_ID, SEED_TOPIC_NAME, SEED_DIAGNOSTIC_TOPIC_ID,
 *   SEED_RESET_PASSWORDS=false (to keep existing passwords)
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Global quiz settings (per your requirements)
const QUIZ_QUESTION_COUNT = 15;
const QUIZ_TIME_PER_QUESTION = 60; // seconds
const QUIZ_ATTEMPTS_ALLOWED = 3;
const QUIZ_SCORING_MODE = 'BEST';

const DEFAULT_PASSWORD = process.env.SEED_PASSWORD || 'password';
const SHOULD_RESET_PASSWORDS =
  (process.env.SEED_RESET_PASSWORDS || 'true').toLowerCase() !== 'false';

const teacherConfig = {
  email: process.env.SEED_TEACHER_EMAIL || 'teacher1@gmail.com',
  displayName: process.env.SEED_TEACHER_NAME || 'Teacher One'
};

const defaultStudents = Array.from({ length: 5 }).map((_, index) => {
  const num = index + 1;
  return {
    email: `student${num}@gmail.com`,
    displayName: `Student ${num}`
  };
});

const studentConfig = (() => {
  const fromEnv = process.env.SEED_STUDENT_EMAILS;
  if (!fromEnv) return defaultStudents;
  return fromEnv
    .split(',')
    .map((email) => email.trim())
    .filter(Boolean)
    .map((email, idx) => ({
      email,
      displayName: `Student ${idx + 1}`
    }));
})();

// Classroom for the demo
const classroomConfig = {
  id: process.env.SEED_CLASSROOM_ID || 'demo-classroom',
  name:
    process.env.SEED_CLASSROOM_NAME ||
    'Gen Math 11 – Q2 Financial Mathematics Demo',
  grade: '11',
  subject: 'General Mathematics – Financial Mathematics',
  joinCode: process.env.SEED_JOIN_CODE || generateJoinCode()
};

/**
 * Quarter 2 topics (MELC 1–11).
 */
const TOPIC_SEEDS = [
  {
    slug: 'gm-q2-las-1-illustrating-simple-and-compound-interest',
    name: 'Illustrating Simple and Compound Interest',
    description:
      'Illustrates and compares simple and compound interest using tables, graphs, and real-life money scenarios.'
  },
  {
    slug: 'gm-q2-las-2-distinguishing-simple-and-compound-interest',
    name: 'Distinguishing Between Simple and Compound Interest',
    description:
      'Highlights differences between simple and compound interest in terms of principal, base of calculation, growth, and returns.'
  },
  {
    slug:
      'gm-q2-las-3-computing-interest-maturity-future-and-present-value',
    name:
      'Computing Interest, Maturity Value, Future Value and Present Value in Simple and Compound Interest Environment',
    description:
      'Uses formulas for simple and compound interest to compute interest, maturity value, future value, and present value.'
  },
  {
    slug: 'gm-q2-las-4-solving-problems-involving-interest',
    name: 'Solving Problems Involving Simple and Compound Interest',
    description:
      'Solves contextual problems involving loans, savings, and investments under simple and compound interest.'
  },
  {
    slug: 'gm-q2-las-5-illustrating-simple-and-general-annuities',
    name: 'Illustrating Simple and General Annuities',
    description:
      'Introduces annuities, payment intervals, and classifications; uses cash-flow diagrams to illustrate simple vs general annuities.'
  },
  {
    slug: 'gm-q2-las-6-distinguishing-simple-and-general-annuities',
    name: 'Distinguishing Between Simple and General Annuities',
    description:
      'Focuses on identifying whether a given situation is a simple annuity or a general annuity based on payment and interest periods.'
  },
  {
    slug:
      'gm-q2-las-7-finding-future-and-present-value-of-annuities',
    name:
      'Finding the Future Value and Present Value of Both Simple Annuities and General Annuities',
    description:
      'Computes future value and present value of simple and general annuities using appropriate formulas.'
  },
  {
    slug:
      'gm-q2-las-8-calculating-fair-market-value-of-cash-flow-stream',
    name:
      'Calculating the Fair Market Value of Cash Flow Stream that Includes Annuity',
    description:
      'Determines fair market value (economic value) of cash flow streams that include annuity payments and lump sums.'
  },
  {
    slug:
      'gm-q2-las-9-calculating-present-value-and-period-of-deferral',
    name:
      'Calculating the Present Value and Period of Deferral of a Deferred Annuity',
    description:
      'Uses time diagrams and formulas to find present value and deferral period of deferred annuities.'
  },
  {
    slug: 'gm-q2-las-10-illustrating-stocks-and-bonds',
    name: 'Illustrating Stocks and Bonds',
    description:
      'Introduces basic concepts, terminology, and simple examples involving stocks and bonds as investment instruments.'
  },
  {
    slug: 'gm-q2-las-11-distinguishing-stocks-and-bonds',
    name: 'Distinguishing Between Stocks and Bonds',
    description:
      'Distinguishes between stocks and bonds in terms of definition, risk, returns, and investor status.'
  }
];

/**
 * Inline material seeds based on LAS 1–11
 * (summaries; not full documents).
 */
const MATERIAL_SEEDS = [
  {
    id: 'gm-q2-las-1',
    topicName: 'Illustrating Simple and Compound Interest',
    title: 'Q2 LAS 1 – Illustrating Simple and Compound Interest',
    description:
      'Learners illustrate simple and compound interest (M11GM-IIa-1) through tables, graphs, and real-life examples.',
    body: `
General Mathematics 11 – Quarter 2
Learning Activity Sheet No. 1: Illustrating Simple and Compound Interest (M11GM-IIa-1)

• Money deposited or borrowed usually earns interest.
• Simple interest is computed only on the principal.
• Compound interest is computed on the principal and on accumulated past interest.

Key terms:
• Principal (P) – amount invested or borrowed.
• Rate (r) – annual interest rate, often in percent.
• Time (t) – length of time money is borrowed or invested.
• Interest (I) – amount paid or earned for using money.
• Maturity value / Future value (F) – total amount after a certain time.

Example comparison on ₱100,000 at 3% for 5 years:
• Simple interest: I = P r t = 100,000(0.03)(5) = ₱15,000, F = ₱115,000.
• Compound interest: F ≈ ₱115,927.41.

Learners complete tables/graphs and answer questions showing how compound interest grows faster than simple interest.
`.trim()
  },
  {
    id: 'gm-q2-las-2',
    topicName: 'Distinguishing Between Simple and Compound Interest',
    title: 'Q2 LAS 2 – Distinguishing Between Simple and Compound Interest',
    description:
      'Learners compare simple vs compound interest in terms of base, principal change, growth, and returns (M11GM-IIa-2).',
    body: `
General Mathematics 11 – Quarter 2
Learning Activity Sheet No. 2: Distinguishing Between Simple and Compound Interest (M11GM-IIa-2)

Simple interest:
• Interest is always computed on the original principal.
• Principal P does not change.
• Growth is linear over time.

Compound interest:
• Interest is computed on principal plus accumulated interest.
• Effective principal increases as interest is added.
• Growth is curved and usually higher than simple interest for the same rate and time.

Learners fill out tables for simple and compound interest, draw graphs, and explain in their own words how the two types of interest differ in practice.
`.trim()
  },
  {
    id: 'gm-q2-las-3',
    topicName:
      'Computing Interest, Maturity Value, Future Value and Present Value in Simple and Compound Interest Environment',
    title:
      'Q2 LAS 3 – Computing Interest, Maturity Value, Future Value and Present Value',
    description:
      'Learners compute I, F, P, t, and r in both simple and compound interest environments using standard formulas (M11GN-IIa-b-1).',
    body: `
General Mathematics 11 – Quarter 2
Learning Activity Sheet No. 3: Computing Interest, Maturity Value, Future Value and Present Value (M11GN-IIa-b-1)

Simple interest:
• F = P(1 + r t)
• I = P r t
• P = F / (1 + r t)

Compound interest:
• F = P(1 + r/n)^(n t)
• I = F − P
• P = F / (1 + r/n)^(n t)

Learners solve problems involving:
• Finding interest and maturity value for a loan or investment.
• Computing required present value to reach a target future value.
• Finding missing rate or time given other quantities.
`.trim()
  },
  {
    id: 'gm-q2-las-4',
    topicName: 'Solving Problems Involving Simple and Compound Interest',
    title: 'Q2 LAS 4 – Solving Problems Involving Simple and Compound Interest',
    description:
      'Learners solve real-life word problems involving simple and compound interest (M11GN-IIb-2).',
    body: `
General Mathematics 11 – Quarter 2
Learning Activity Sheet No. 4: Solving Problems Involving Simple and Compound Interest (M11GN-IIb-2)

Focus:
• Apply simple and compound interest formulas to situations like loans, medical expenses, tuition, and savings.

Sample problem:
• Michael borrows ₱25,000 at 5% simple interest for 3 years. How much interest will he pay?
• Dominic invests ₱40,000 at 5% compounded bimonthly for 3 years. How much compound interest does he earn?

Learners identify whether to use simple or compound interest and explain each step.
`.trim()
  },
  {
    id: 'gm-q2-las-5',
    topicName: 'Illustrating Simple and General Annuities',
    title: 'Q2 LAS 5 – Illustrating Simple and General Annuities',
    description:
      'Learners illustrate simple and general annuities using timelines, payments, and compounding (M11GM-IIc-1).',
    body: `
General Mathematics 11 – Quarter 2
Learning Activity Sheet No. 5: Illustrating Simple and General Annuities (M11GM-IIc-1)

Key ideas:
• Annuity – sequence of equal payments at equal intervals.
• Payment interval – time between payments.
• Term of annuity – time between first and last payment intervals.
• Regular payment (A or R) – amount paid or received each interval.
• Simple annuity – payment interval equals interest period.
• General annuity – payment interval differs from interest period.

Learners draw cash-flow diagrams for simple and general annuities, showing payment times, the value of each payment, and the compounding structure.
`.trim()
  },
  {
    id: 'gm-q2-las-6',
    topicName: 'Distinguishing Between Simple and General Annuities',
    title: 'Q2 LAS 6 – Distinguishing Between Simple and General Annuities',
    description:
      'Learners decide whether a given scenario is a simple or general annuity by comparing payment intervals and interest periods (M11GM-IIc-2).',
    body: `
General Mathematics 11 – Quarter 2
Learning Activity Sheet No. 6: Distinguishing Between Simple and General Annuities (M11GM-IIc-2)

Simple annuity:
• Payment interval = interest period.

General annuity:
• Payment interval ≠ interest period.

Learners classify real-life situations (savings plans, loan repayments, investments) as simple or general annuities and justify their answers using the relationship between payment intervals and interest periods.
`.trim()
  },
  {
    id: 'gm-q2-las-7',
    topicName:
      'Finding the Future Value and Present Value of Both Simple Annuities and General Annuities',
    title:
      'Q2 LAS 7 – Finding the Future Value and Present Value of Annuities',
    description:
      'Learners find future value and present value of simple and general annuities using appropriate formulas (M11GN-IIc-d-1).',
    body: `
General Mathematics 11 – Quarter 2
Learning Activity Sheet No. 7: Finding the Future Value and Present Value of Both Simple Annuities and General Annuities (M11GN-IIc-d-1)

Ordinary annuity (payments at end of period):
• F = A * [ (1 + j)^n − 1 ] / j
• P = A * [ 1 − (1 + j)^−n ] / j

Where:
• A = regular payment
• j = periodic interest rate
• n = total number of payments

Learners:
• Identify A, j, and n from the problem.
• Compute future value F and present value P for simple and general annuities.
`.trim()
  },
  {
    id: 'gm-q2-las-8',
    topicName:
      'Calculating the Fair Market Value of Cash Flow Stream that Includes Annuity',
    title:
      'Q2 LAS 8 – Calculating the Fair Market Value of Cash Flow Streams with Annuity',
    description:
      'Learners compute fair market value of offers that combine lump sums and annuity-type payments (M11GM-IId-2).',
    body: `
General Mathematics 11 – Quarter 2
Learning Activity Sheet No. 8: Calculating the Fair Market Value of Cash Flow Stream that Includes Annuity (M11GM-IId-2)

Fair market value (FMV):
• Single amount equivalent to all cash flows at a chosen focal date.

Steps:
1. Draw a time diagram showing all payments.
2. Choose a focal date (often t = 0).
3. Find the present value of each cash flow at the focal date.
4. Sum these present values to obtain the FMV.

Learners compare different offers for a lot or investment by computing and comparing their fair market values.
`.trim()
  },
  {
    id: 'gm-q2-las-9',
    topicName:
      'Calculating the Present Value and Period of Deferral of a Deferred Annuity',
    title:
      'Q2 LAS 9 – Calculating the Present Value and Period of Deferral of a Deferred Annuity',
    description:
      'Learners compute present value and deferral period for deferred annuities using artificial payments and time diagrams (M11GM-IId-3).',
    body: `
General Mathematics 11 – Quarter 2
Learning Activity Sheet No. 9: Calculating the Present Value and Period of Deferral of a Deferred Annuity (M11GM-IId-3)

Deferred annuity:
• Payments start after a delay (deferral period).
• We may imagine “artificial payments” during the deferral period to help with computation.

Learners:
• Draw time diagrams marking the deferral period and payment period.
• Identify the number of deferred periods and actual payment periods.
• Use annuity formulas to compute present value and time of deferral.
`.trim()
  },
  {
    id: 'gm-q2-las-10',
    topicName: 'Illustrating Stocks and Bonds',
    title: 'Q2 LAS 10 – Illustrating Stocks and Bonds',
    description:
      'Learners explore basic ideas, terms, and simple computations involving stocks and bonds (M11GM-IIe-1).',
    body: `
General Mathematics 11 – Quarter 2
Learning Activity Sheet No. 10: Illustrating Stocks and Bonds (M11GM-IIe-1)

Stocks:
• Represent ownership in a corporation.
• Key terms: dividend, dividend per share, par value, market value, stock yield.

Bonds:
• Represent long-term debt; investor is a lender.
• Key terms: face value, coupon rate, coupon payment, current yield, maturity date.

Learners solve simple problems:
• Compute dividend per share.
• Compute coupon payments and current yield for bonds.
• Interpret results in terms of risk and return.
`.trim()
  },
  {
    id: 'gm-q2-las-11',
    topicName: 'Distinguishing Between Stocks and Bonds',
    title: 'Q2 LAS 11 – Distinguishing Between Stocks and Bonds',
    description:
      'Learners distinguish stocks from bonds in terms of risk, returns, status of investors, and appropriate use (M11GM-IIe-2).',
    body: `
General Mathematics 11 – Quarter 2
Learning Activity Sheet No. 11: Distinguishing Between Stocks and Bonds (M11GM-IIe-2)

Background:
• Investment helps economic growth; investors can place money in different financial instruments.
• Stocks and bonds are two major asset classes that behave differently in terms of risk and return.

Key comparisons:
• Definition – Stocks are equity: investors become part-owners of the company. Bonds are debt: investors lend money to the company or government.
• Status of holders – Stockholders are owners with voting rights; bondholders are creditors with priority in case of liquidation.
• Level of risk – Stocks usually carry higher risk and more volatile returns; bonds usually carry lower risk and more stable income.
• Form of returns – Stocks may pay dividends based on profits and can gain or lose value. Bonds pay interest (coupons) at a stated rate on the face value.
• Potential returns – Stocks can offer higher potential gains over the long term. Bonds offer lower but more predictable returns.
• Major risks – Stocks face market and business risk; bonds face interest rate and inflation risk.
• Additional benefits – Stockholders may vote on company decisions; bondholders are prioritized for payment of interest and principal.

Learners analyze diagrams, complete tables comparing stocks and bonds, answer conceptual questions, and reflect on which investment might be suitable for different types of investors (e.g., long-term growth vs fixed income).
`.trim()
  }
];

function generateJoinCode() {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let result = '';
  for (let i = 0; i < 6; i += 1) {
    result += alphabet[Math.floor(Math.random() * alphabet.length)];
  }
  return result;
}

function slugify(value) {
  return (
    value
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .replace(/-{2,}/g, '-')
      .slice(0, 64) || `material-${Date.now()}`
  );
}

/**
 * Normalize Firestore timestamps, Date objects, ISO strings, or numbers to millis.
 */
function toMillis(value, fallback = Date.now()) {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (value instanceof Date && Number.isFinite(value.getTime())) {
    return value.getTime();
  }
  if (value && typeof value.toMillis === 'function') return value.toMillis();
  if (typeof value === 'string') {
    const parsed = Date.parse(value);
    if (!Number.isNaN(parsed)) return parsed;
  }
  return fallback;
}

function resolveProjectId() {
  const fromServiceAccount = (() => {
    const saPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
    if (!saPath) return null;
    try {
      const buf = fs.readFileSync(saPath, 'utf8');
      const json = JSON.parse(buf);
      return json.project_id || null;
    } catch (error) {
      console.warn(
        `[seed] Could not read service account at ${saPath}: ${error.message}`
      );
      return null;
    }
  })();

  const fromFirebaserc = (() => {
    try {
      const rcPath = path.resolve(__dirname, '..', '..', '.firebaserc');
      if (!fs.existsSync(rcPath)) return null;
      const parsed = JSON.parse(fs.readFileSync(rcPath, 'utf8'));
      return parsed?.projects?.default || null;
    } catch (error) {
      console.warn('[seed] Ignoring invalid .firebaserc:', error.message);
      return null;
    }
  })();

  const fromEnv =
    process.env.SEED_PROJECT_ID ||
    process.env.GCLOUD_PROJECT ||
    process.env.GOOGLE_CLOUD_PROJECT ||
    process.env.GCP_PROJECT;
  const fromFirebaseConfig = (() => {
    const cfg = process.env.FIREBASE_CONFIG;
    if (!cfg) return null;
    try {
      const parsed = typeof cfg === 'string' ? JSON.parse(cfg) : cfg;
      return parsed?.projectId || null;
    } catch (error) {
      console.warn('[seed] Ignoring invalid FIREBASE_CONFIG JSON:', error.message);
      return null;
    }
  })();
  return (
    fromEnv ||
    fromFirebaseConfig ||
    fromServiceAccount ||
    fromFirebaserc ||
    null
  );
}

function ensureProjectAndCredentials() {
  const usingEmulator =
    Boolean(process.env.FIRESTORE_EMULATOR_HOST) ||
    Boolean(process.env.FIREBASE_AUTH_EMULATOR_HOST);
  let projectId = resolveProjectId();
  if (!projectId) {
    projectId = 'demo-seed-project';
    console.warn(
      `[seed] Project ID not found in env/service account/.firebaserc; defaulting to "${projectId}". Set SEED_PROJECT_ID/GCLOUD_PROJECT or FIREBASE_CONFIG.projectId to use a real project.`
    );
  }

  if (admin.apps.length === 0) {
    admin.initializeApp({ projectId });
  }

  const finalProjectId = admin.app().options?.projectId || projectId;

  const hasServiceAccount = Boolean(process.env.GOOGLE_APPLICATION_CREDENTIALS);
  if (!usingEmulator && !hasServiceAccount) {
    throw new Error(
      '[seed] No credentials found. Set GOOGLE_APPLICATION_CREDENTIALS to a service account JSON, or configure emulator hosts (FIRESTORE_EMULATOR_HOST and FIREBASE_AUTH_EMULATOR_HOST) with a project ID.'
    );
  }

  return { projectId: finalProjectId };
}

function buildTopicConfigs() {
  return TOPIC_SEEDS.map((seed, index) => {
    const slug = seed.slug || slugify(seed.name);
    const overrideId = index === 0 ? process.env.SEED_TOPIC_ID : null;
    const overrideName = index === 0 ? process.env.SEED_TOPIC_NAME : null;
    return {
      id: overrideId || `${classroomConfig.id}-${slug}`,
      name: overrideName || seed.name,
      description: seed.description,
      slug
    };
  });
}

function pickDiagnosticTopic(topics) {
  if (!Array.isArray(topics) || topics.length === 0) return null;
  const desiredId = process.env.SEED_DIAGNOSTIC_TOPIC_ID;
  if (desiredId) {
    const found = topics.find((t) => t.id === desiredId || t.slug === desiredId);
    if (found) return found;
    console.warn(
      `[seed] SEED_DIAGNOSTIC_TOPIC_ID=${desiredId} not found; falling back to first topic.`
    );
  }
  return topics[0];
}

async function ensurePasswordUser(auth, { email, displayName }, role) {
  const desired = { email, password: DEFAULT_PASSWORD, displayName };
  try {
    const existing = await auth.getUserByEmail(email);
    const updates = {};
    if (displayName && existing.displayName !== displayName) {
      updates.displayName = displayName;
    }
    if (SHOULD_RESET_PASSWORDS) {
      updates.password = DEFAULT_PASSWORD;
    }
    if (Object.keys(updates).length > 0) {
      await auth.updateUser(existing.uid, updates);
    }
    console.log(`[seed][${role}] Using existing auth user ${email}`);
    return {
      uid: existing.uid,
      email: existing.email || email,
      displayName: updates.displayName || existing.displayName || displayName,
      createdAt: toMillis(existing.metadata?.creationTime, Date.now()),
      wasCreated: false
    };
  } catch (error) {
    if (error?.code !== 'auth/user-not-found') {
      throw error;
    }
    const created = await auth.createUser(desired);
    console.log(`[seed][${role}] Created auth user ${email}`);
    return {
      uid: created.uid,
      email: created.email || email,
      displayName: created.displayName || displayName,
      createdAt: toMillis(created.metadata?.creationTime, Date.now()),
      wasCreated: true
    };
  }
}

async function upsertTeacherProfile(db, teacher) {
  const ref = db.collection('teachers').doc(teacher.uid);
  const existing = await ref.get();
  const createdAt =
    existing.exists && existing.get('createdAt') !== undefined
      ? toMillis(existing.get('createdAt'), teacher.createdAt || Date.now())
      : teacher.createdAt || Date.now();
  await ref.set(
    {
      displayName: teacher.displayName,
      email: teacher.email,
      createdAt,
      lastActive: Date.now()
    },
    { merge: true }
  );
}

async function upsertStudentProfile(db, student) {
  const ref = db.collection('students').doc(student.uid);
  const existing = await ref.get();
  const createdAt =
    existing.exists && existing.get('createdAt') !== undefined
      ? toMillis(existing.get('createdAt'), student.createdAt || Date.now())
      : student.createdAt || Date.now();
  await ref.set(
    {
      displayName: student.displayName,
      email: student.email,
      createdAt
    },
    { merge: true }
  );
}

async function upsertClassroom(db, teacherId, studentIds) {
  const ref = db.collection('classrooms').doc(classroomConfig.id);
  const existing = await ref.get();
  const createdAt =
    existing.exists && existing.get('createdAt') !== undefined
      ? toMillis(existing.get('createdAt'), Date.now())
      : Date.now();
  const joinCode =
    existing.exists && existing.get('joinCode')
      ? existing.get('joinCode')
      : classroomConfig.joinCode;
  const existingStudents =
    existing.exists && Array.isArray(existing.get('students'))
      ? existing.get('students').filter(Boolean)
      : [];
  const uniqueStudents = Array.from(
    new Set([...existingStudents, ...studentIds])
  ).filter(Boolean);
  await ref.set(
    {
      teacherId,
      name: classroomConfig.name,
      grade: classroomConfig.grade,
      subject: classroomConfig.subject,
      joinCode,
      createdAt,
      updatedAt: Date.now(),
      isArchived: false,
      archived: false,
      archivedAt: null,
      students: uniqueStudents
    },
    { merge: true }
  );
}

/**
 * Base question banks (by content cluster).
 * LAS-specific banks will reuse these, but each LAS gets its own wrapper.
 */

function buildInterestBank(label) {
  const questions = [
    {
      stem: 'Which formula correctly computes simple interest?',
      choices: [
        'I = P r t',
        'F = P(1 + r/n)^(nt)',
        'P = F / (1 + r t)',
        'I = F − P'
      ],
      correct: ['I = P r t']
    },
    {
      stem:
        'If interest is added back to the principal each period, what type of interest is this?',
      choices: ['Simple interest', 'Compound interest', 'Discounting', 'Amortization'],
      correct: ['Compound interest']
    },
    {
      stem:
        'A loan of ₱10,000 at 6% simple interest for 3 years earns how much interest?',
      choices: ['₱600', '₱1,800', '₱2,400', '₱3,600'],
      correct: ['₱1,800']
    },
    {
      stem:
        'At 8% compounded annually, which amount is closest to the future value of ₱5,000 after 5 years?',
      choices: ['₱6,000', '₱7,346', '₱7,999', '₱8,250'],
      correct: ['₱7,346']
    },
    {
      stem:
        'True or False: More compounding periods per year generally increase future value (holding rate and time constant).',
      choices: ['True', 'False'],
      correct: ['True']
    },
    {
      stem:
        'Which variable represents the total number of years in simple interest problems?',
      choices: ['P', 'r', 't', 'n'],
      correct: ['t']
    },
    {
      stem:
        'In simple interest, the base for computing interest stays constant. In compound interest, the base is:',
      choices: ['Decreasing', 'Increasing', 'Zero', 'Negative'],
      correct: ['Increasing']
    },
    {
      stem:
        'Which is larger after 4 years on ₱2,000 at 9%: simple interest or 9% compounded annually?',
      choices: [
        'Simple interest',
        'Compound interest',
        'They are equal',
        'Cannot be determined'
      ],
      correct: ['Compound interest']
    },
    {
      stem:
        'The future value of ₱1,500 at 5% simple interest after 6 years is closest to:',
      choices: ['₱1,950', '₱1,995', '₱2,000', '₱2,250'],
      correct: ['₱1,950']
    },
    {
      stem: 'Present value is best described as:',
      choices: [
        'The value of a future sum discounted back to today',
        'The interest earned each year',
        'The maturity value of a loan',
        'The coupon rate of a bond'
      ],
      correct: ['The value of a future sum discounted back to today']
    },
    {
      stem:
        'If the nominal rate is 12% with monthly compounding, the periodic (monthly) rate is:',
      choices: ['12%', '1%', '0.12%', 'Cannot be known'],
      correct: ['1%']
    },
    {
      stem: 'Which is the maturity value for simple interest?',
      choices: ['Principal only', 'Principal + interest', 'Interest only', 'Principal − interest'],
      correct: ['Principal + interest']
    },
    {
      stem:
        'True or False: Discounting uses the same rate and time concepts as compounding but in reverse.',
      choices: ['True', 'False'],
      correct: ['True']
    },
    {
      stem:
        'A deposit grows from ₱2,500 to ₱3,150 in one year. The implied simple interest rate is closest to:',
      choices: ['12%', '20%', '26%', '32%'],
      correct: ['26%']
    },
    {
      stem: 'Which scenario best matches simple interest?',
      choices: [
        'Credit card debt adding interest monthly to balance',
        'Bank savings account with quarterly compounding',
        'A short-term note that pays interest only on principal',
        'Dividend reinvestment plan'
      ],
      correct: ['A short-term note that pays interest only on principal']
    }
  ];

  return questions.slice(0, QUIZ_QUESTION_COUNT).map((q, index) => ({
    id: `q${index + 1}`,
    quizId: '',
    type: index === 4 || index === 12 ? 'tf' : 'mcq',
    stem: `${label}: ${q.stem}`,
    choices: q.choices,
    answerKey: q.correct,
    explanation: '',
    timeLimitSeconds: QUIZ_TIME_PER_QUESTION
  }));
}

function buildAnnuityBank(label) {
  const questions = [
    {
      stem: 'What is an annuity?',
      choices: [
        'A one-time payment',
        'A sequence of equal payments at equal intervals',
        'A type of stock investment',
        'A bank service charge'
      ],
      correct: ['A sequence of equal payments at equal intervals']
    },
    {
      stem: 'In a simple annuity, the payment interval is:',
      choices: [
        'Shorter than the interest period',
        'Longer than the interest period',
        'Equal to the interest period',
        'Unrelated to the interest period'
      ],
      correct: ['Equal to the interest period']
    },
    {
      stem:
        'In a general annuity, the payment interval compared to the interest period is usually:',
      choices: [
        'Exactly equal',
        'Different (not the same length)',
        'Zero',
        'Negative'
      ],
      correct: ['Different (not the same length)']
    },
    {
      stem:
        'Which symbol is commonly used for the regular annuity payment in formulas?',
      choices: ['P', 'F', 'A (or R)', 'j'],
      correct: ['A (or R)']
    },
    {
      stem:
        'True or False: An ordinary annuity has payments at the beginning of each period.',
      choices: ['True', 'False'],
      correct: ['False']
    },
    {
      stem:
        'For an ordinary annuity, the future value F can be computed using which structure?',
      choices: [
        'F = P(1 + r t)',
        'F = A * [ (1 + j)^n − 1 ] / j',
        'F = A(1 + r/n)^(nt)',
        'F = A * [ 1 − (1 + j)^−n ] / j'
      ],
      correct: ['F = A * [ (1 + j)^n − 1 ] / j']
    },
    {
      stem:
        'The present value P of an ordinary annuity of A per period for n periods at periodic rate j is given by:',
      choices: [
        'P = A * [1 − (1 + j)^−n] / j',
        'P = A * [ (1 + j)^n − 1 ] / j',
        'P = A n j',
        'P = A / (1 + j)^n'
      ],
      correct: ['P = A * [1 − (1 + j)^−n] / j']
    },
    {
      stem:
        'A general annuity often requires adjusting which of the following?',
      choices: [
        'The payment A only',
        'The number of payments n only',
        'The periodic rate j only',
        'Both j and n to reflect the true payment schedule'
      ],
      correct: ['Both j and n to reflect the true payment schedule']
    },
    {
      stem:
        'If payments are made monthly but interest is compounded quarterly, the annuity is:',
      choices: [
        'Simple annuity',
        'General annuity',
        'Not an annuity',
        'Impossible to classify'
      ],
      correct: ['General annuity']
    },
    {
      stem:
        'Which diagram feature best helps distinguish simple from general annuities?',
      choices: [
        'Length of arrows',
        'Timing of payments relative to interest periods',
        'Color of the diagram',
        'Labels for P and F only'
      ],
      correct: ['Timing of payments relative to interest periods']
    },
    {
      stem:
        'Deferred annuity problems are often solved by thinking in terms of:',
      choices: [
        'Artificial payments during deferral',
        'Ignoring the deferral period',
        'Using simple interest only',
        'Avoiding diagrams'
      ],
      correct: ['Artificial payments during deferral']
    },
    {
      stem:
        'Fair market value of a cash-flow stream is conceptually equal to:',
      choices: [
        'The sum of all future payments',
        'The sum of present values of all payments at a focal date',
        'Only the last payment',
        'The first payment times the number of payments'
      ],
      correct: ['The sum of present values of all payments at a focal date']
    },
    {
      stem:
        'True or False: In a deferred annuity, payments start immediately at time zero.',
      choices: ['True', 'False'],
      correct: ['False']
    },
    {
      stem:
        'Which of the following is NOT typically required to compute the present value of an ordinary annuity?',
      choices: ['Regular payment A', 'Number of payments n', 'Periodic rate j', 'Maturity date of a bond'],
      correct: ['Maturity date of a bond']
    },
    {
      stem:
        'Which statement about simple vs general annuities is MOST accurate?',
      choices: [
        'Simple annuities never involve compounding',
        'General annuities cannot be solved using present value formulas',
        'Simple annuities have equal payment and interest periods; general annuities do not',
        'Both are the same'
      ],
      correct: [
        'Simple annuities have equal payment and interest periods; general annuities do not'
      ]
    }
  ];

  return questions.slice(0, QUIZ_QUESTION_COUNT).map((q, index) => ({
    id: `q${index + 1}`,
    quizId: '',
    type: index === 4 || index === 12 ? 'tf' : 'mcq',
    stem: `${label}: ${q.stem}`,
    choices: q.choices,
    answerKey: q.correct,
    explanation: '',
    timeLimitSeconds: QUIZ_TIME_PER_QUESTION
  }));
}

function buildStocksBondsBank(label) {
  const questions = [
    {
      stem: 'A share of stock represents:',
      choices: [
        'A loan to a company',
        'Partial ownership in a company',
        'A bank deposit',
        'An insurance policy'
      ],
      correct: ['Partial ownership in a company']
    },
    {
      stem: 'Dividend per share is computed by:',
      choices: [
        'Total dividends ÷ number of shares',
        'Par value ÷ total dividends',
        'Market value ÷ number of shares',
        'Coupon rate ÷ face value'
      ],
      correct: ['Total dividends ÷ number of shares']
    },
    {
      stem: 'Market value of a stock is:',
      choices: [
        'The value printed on the certificate',
        'The current price at which it is traded',
        'The original issue price',
        'Always equal to par value'
      ],
      correct: ['The current price at which it is traded']
    },
    {
      stem: 'A bond is best described as:',
      choices: [
        'Evidence of ownership',
        'Evidence of long-term debt',
        'A type of savings account',
        'A kind of currency'
      ],
      correct: ['Evidence of long-term debt']
    },
    {
      stem:
        'True or False: Bondholders are creditors of the company, not owners.',
      choices: ['True', 'False'],
      correct: ['True']
    },
    {
      stem: 'The coupon rate of a bond is applied to:',
      choices: [
        'Market value',
        'Face (par) value',
        'Dividend per share',
        'Present value of an annuity'
      ],
      correct: ['Face (par) value']
    },
    {
      stem:
        'A bond with ₱100,000 face value and 8% coupon rate pays annual interest of:',
      choices: ['₱800', '₱8,000', '₱80,000', '₱1,000'],
      correct: ['₱8,000']
    },
    {
      stem: 'Current yield of a bond is:',
      choices: [
        'Annual interest ÷ current price',
        'Annual interest ÷ face value',
        'Current price ÷ face value',
        'Coupon rate ÷ current price'
      ],
      correct: ['Annual interest ÷ current price']
    },
    {
      stem:
        'If a stock has a dividend per share of ₱5 and market value of ₱100, the stock yield ratio is:',
      choices: ['0.5%', '5%', '10%', '20%'],
      correct: ['5%']
    },
    {
      stem:
        'Investors who buy stocks usually expect to earn income primarily from:',
      choices: [
        'Dividends and possible price appreciation',
        'Fixed coupon payments only',
        'Guaranteed interest',
        'Bank service charges'
      ],
      correct: ['Dividends and possible price appreciation']
    },
    {
      stem:
        'Compared with stocks, bonds generally offer:',
      choices: [
        'Lower risk and more predictable income',
        'Higher risk and no income',
        'Exactly the same risk',
        'No relation to risk'
      ],
      correct: ['Lower risk and more predictable income']
    },
    {
      stem:
        'A company using bonds instead of stocks to raise money is primarily:',
      choices: [
        'Selling ownership',
        'Borrowing money',
        'Collecting dividends',
        'Buying insurance'
      ],
      correct: ['Borrowing money']
    },
    {
      stem:
        'True or False: A stockholder is guaranteed a fixed dividend every year.',
      choices: ['True', 'False'],
      correct: ['False']
    },
    {
      stem:
        'Which of the following is commonly associated with bonds but not with stocks?',
      choices: [
        'Voting rights',
        'Maturity date',
        'Dividends',
        'Ownership share'
      ],
      correct: ['Maturity date']
    },
    {
      stem:
        'Which statement best compares stocks and bonds?',
      choices: [
        'Stocks and bonds always yield the same return.',
        'Stocks represent ownership; bonds represent a loan.',
        'Stocks have coupon payments; bonds have dividends.',
        'There is no difference between them.'
      ],
      correct: ['Stocks represent ownership; bonds represent a loan.']
    }
  ];

  return questions.slice(0, QUIZ_QUESTION_COUNT).map((q, index) => ({
    id: `q${index + 1}`,
    quizId: '',
    type: index === 4 || index === 12 ? 'tf' : 'mcq',
    stem: `${label}: ${q.stem}`,
    choices: q.choices,
    answerKey: q.correct,
    explanation: '',
    timeLimitSeconds: QUIZ_TIME_PER_QUESTION
  }));
}

/**
 * LAS-specific wrappers.
 * Right now they reuse the appropriate base bank, but each LAS has
 * its own function so you can later customize per LAS easily.
 */

function buildLas1Bank(label) {
  return buildInterestBank(label || 'LAS 1 – Illustrating Simple and Compound Interest');
}

function buildLas2Bank(label) {
  return buildInterestBank(label || 'LAS 2 – Distinguishing Between Simple and Compound Interest');
}

function buildLas3Bank(label) {
  return buildInterestBank(
    label ||
      'LAS 3 – Computing Interest, Maturity Value, Future Value and Present Value'
  );
}

function buildLas4Bank(label) {
  return buildInterestBank(
    label || 'LAS 4 – Solving Problems Involving Simple and Compound Interest'
  );
}

function buildLas5Bank(label) {
  return buildAnnuityBank(label || 'LAS 5 – Illustrating Simple and General Annuities');
}

function buildLas6Bank(label) {
  return buildAnnuityBank(
    label || 'LAS 6 – Distinguishing Between Simple and General Annuities'
  );
}

function buildLas7Bank(label) {
  return buildAnnuityBank(
    label ||
      'LAS 7 – Finding the Future Value and Present Value of Simple and General Annuities'
  );
}

function buildLas8Bank(label) {
  return buildAnnuityBank(
    label ||
      'LAS 8 – Calculating the Fair Market Value of Cash Flow Stream that Includes Annuity'
  );
}

function buildLas9Bank(label) {
  return buildAnnuityBank(
    label ||
      'LAS 9 – Calculating the Present Value and Period of Deferral of a Deferred Annuity'
  );
}

function buildLas10Bank(label) {
  return buildStocksBondsBank(label || 'LAS 10 – Illustrating Stocks and Bonds');
}

function buildLas11Bank(label) {
  return buildStocksBondsBank(label || 'LAS 11 – Distinguishing Between Stocks and Bonds');
}

function inferLasNumberFromTopic(topic) {
  const slug = topic.slug || slugify(topic.name || '');
  const match = slug.match(/las-(\d+)/);
  if (match) {
    const n = parseInt(match[1], 10);
    if (!Number.isNaN(n)) return n;
  }
  return null;
}

/**
 * Build topic-specific quiz questions based on LAS number.
 */
function buildTopicQuizQuestions(topic) {
  const las = inferLasNumberFromTopic(topic);
  const label = topic.name || '';

  switch (las) {
    case 1:
      return buildLas1Bank(label);
    case 2:
      return buildLas2Bank(label);
    case 3:
      return buildLas3Bank(label);
    case 4:
      return buildLas4Bank(label);
    case 5:
      return buildLas5Bank(label);
    case 6:
      return buildLas6Bank(label);
    case 7:
      return buildLas7Bank(label);
    case 8:
      return buildLas8Bank(label);
    case 9:
      return buildLas9Bank(label);
    case 10:
      return buildLas10Bank(label);
    case 11:
      return buildLas11Bank(label);
    default:
      // Fallback: cluster-based routing
      if (label.includes('Interest')) return buildInterestBank(label);
      if (label.includes('Annuities') || label.includes('Annuity')) {
        return buildAnnuityBank(label);
      }
      return buildStocksBondsBank(label);
  }
}

/**
 * Classroom-wide pre/post test questions (15 items).
 * Kind: 'pre' or 'post'.
 * Uses cluster banks as a balanced mix.
 */
function buildClassroomAssessmentQuestions(kind) {
  const label =
    kind === 'pre'
      ? 'Q2 Classroom Pre-Test (All Topics)'
      : 'Q2 Classroom Post-Test (All Topics)';

  // Use first 5 from each bank to get 15 items.
  const interest = buildInterestBank(label).slice(0, 5);
  const annuity = buildAnnuityBank(label).slice(0, 5);
  const stocks = buildStocksBondsBank(label).slice(0, 5);

  const raw = [...interest, ...annuity, ...stocks];

  return raw.map((q, index) => ({
    ...q,
    id: `q${index + 1}`,
    quizId: ''
  }));
}

async function upsertTopics(db, teacherId, topics) {
  const createdTopics = [];
  for (const topic of topics) {
    const ref = db.collection('topics').doc(topic.id);
    const existing = await ref.get();
    const createdAt =
      existing.exists && existing.get('createdAt') !== undefined
        ? toMillis(existing.get('createdAt'), Date.now())
        : Date.now();
    await ref.set(
      {
        classroomId: classroomConfig.id,
        teacherId,
        name: topic.name,
        description: topic.description,
        slug: topic.slug,
        createdAt,
        updatedAt: Date.now(),
        isArchived: false,
        archived: false,
        archivedAt: null
      },
      { merge: true }
    );
    createdTopics.push(topic);
  }
  return createdTopics;
}

/**
 * Topic quizzes: one quiz per topic + assignment
 * - 15 questions (from appropriate LAS bank)
 * - 3 attempts, scoringMode: 'BEST'
 */
async function seedTopicQuizzesAndAssignments(db, teacherId, topics) {
  const now = Date.now();
  const defaultCloseAt = now + 14 * 24 * 60 * 60 * 1000; // 14 days
  let quizzesCreated = 0;
  let assignmentsCreated = 0;

  for (const topic of topics) {
    const quizId = `seed-topic-quiz-${topic.id}`;
    const baseQuestions = buildTopicQuizQuestions(topic);
    const questions = baseQuestions.map((q) => ({
      ...q,
      quizId,
      id: `${quizId}-${q.id}`
    }));

    const quizRef = db.collection('quizzes').doc(quizId);
    const existingQuiz = await quizRef.get();
    const quizCreatedAt =
      existingQuiz.exists && existingQuiz.get('createdAt') !== undefined
        ? toMillis(existingQuiz.get('createdAt'), now)
        : now;
    await quizRef.set(
      {
        teacherId,
        classroomId: classroomConfig.id,
        topicId: topic.id,
        title: `${topic.name}: Topic Quiz`,
        defaultTimePerQ: QUIZ_TIME_PER_QUESTION,
        shuffle: true,
        createdAt: quizCreatedAt,
        updatedAt: now,
        questionCount: questions.length,
        questionsJson: JSON.stringify(questions),
        category: 'STANDARD',
        isArchived: false,
        archivedAt: null
      },
      { merge: true }
    );
    quizzesCreated += 1;

    const assignmentId = `seed-topic-assignment-${topic.id}`;
    const assignmentRef = db.collection('assignments').doc(assignmentId);
    const existingAssignment = await assignmentRef.get();
    const assignmentCreatedAt =
      existingAssignment.exists && existingAssignment.get('createdAt') !== undefined
        ? toMillis(existingAssignment.get('createdAt'), now)
        : now;
    const openAt =
      existingAssignment.exists && existingAssignment.get('openAt') !== undefined
        ? toMillis(existingAssignment.get('openAt'), now)
        : now;
    const closeAt =
      existingAssignment.exists && existingAssignment.get('closeAt') !== undefined
        ? toMillis(existingAssignment.get('closeAt'), defaultCloseAt)
        : defaultCloseAt;
    await assignmentRef.set(
      {
        quizId,
        classroomId: classroomConfig.id,
        topicId: topic.id,
        openAt,
        closeAt,
        attemptsAllowed: QUIZ_ATTEMPTS_ALLOWED,
        scoringMode: QUIZ_SCORING_MODE,
        revealAfterSubmit: true,
        createdAt: assignmentCreatedAt,
        updatedAt: now,
        isArchived: false,
        archived: false,
        archivedAt: null
      },
      { merge: true }
    );
    assignmentsCreated += 1;
  }

  return { quizzesCreated, assignmentsCreated };
}

/**
 * Classroom-wide pre/post tests:
 * - Not tied to a specific topic (topicId: null).
 * - 15 questions from all topic areas.
 */
async function seedClassroomPrePostQuizzes(db, teacherId, topics) {
  const now = Date.now();
  const defaultCloseAt = now + 14 * 24 * 60 * 60 * 1000;
  let quizzesCreated = 0;
  let assignmentsCreated = 0;

  const diagnosticTopic = pickDiagnosticTopic(topics);
  if (!diagnosticTopic) {
    throw new Error(
      '[seed] Cannot create classroom pre/post tests without at least one topic.'
    );
  }

  const quizDefs = [
    {
      kind: 'pre',
      title: 'Gen Math 11 – Q2 Pre-Test (Financial Mathematics)',
      category: 'PRE_TEST'
    },
    {
      kind: 'post',
      title: 'Gen Math 11 – Q2 Post-Test (Financial Mathematics)',
      category: 'POST_TEST'
    }
  ];

  for (const quizDef of quizDefs) {
    const questions = buildClassroomAssessmentQuestions(quizDef.kind);
    const quizId = `seed-classroom-quiz-${classroomConfig.id}-${quizDef.kind}`;
    const quizQuestions = questions.map((q) => ({
      ...q,
      quizId,
      id: `${quizId}-${q.id}`
    }));

    const quizRef = db.collection('quizzes').doc(quizId);
    const existingQuiz = await quizRef.get();
    const quizCreatedAt =
      existingQuiz.exists && existingQuiz.get('createdAt') !== undefined
        ? toMillis(existingQuiz.get('createdAt'), now)
        : now;
    await quizRef.set(
      {
        teacherId,
        classroomId: classroomConfig.id,
        topicId: diagnosticTopic.id, // UI expects a topic for diagnostic tests
        title: quizDef.title,
        defaultTimePerQ: QUIZ_TIME_PER_QUESTION,
        shuffle: true,
        createdAt: quizCreatedAt,
        updatedAt: now,
        questionCount: quizQuestions.length,
        questionsJson: JSON.stringify(quizQuestions),
        category: quizDef.category,
        isArchived: false,
        archivedAt: null
      },
      { merge: true }
    );
    quizzesCreated += 1;

    const assignmentId = `seed-classroom-assignment-${classroomConfig.id}-${quizDef.kind}`;
    const assignmentRef = db.collection('assignments').doc(assignmentId);
    const existingAssignment = await assignmentRef.get();
    const assignmentCreatedAt =
      existingAssignment.exists && existingAssignment.get('createdAt') !== undefined
        ? toMillis(existingAssignment.get('createdAt'), now)
        : now;
    const openAt =
      existingAssignment.exists && existingAssignment.get('openAt') !== undefined
        ? toMillis(existingAssignment.get('openAt'), now)
        : now;
    const closeAt =
      existingAssignment.exists && existingAssignment.get('closeAt') !== undefined
        ? toMillis(existingAssignment.get('closeAt'), defaultCloseAt)
        : defaultCloseAt;
    await assignmentRef.set(
      {
        quizId,
        classroomId: classroomConfig.id,
        topicId: diagnosticTopic.id,
        openAt,
        closeAt,
        attemptsAllowed: QUIZ_ATTEMPTS_ALLOWED,
        scoringMode: QUIZ_SCORING_MODE,
        revealAfterSubmit: true,
        createdAt: assignmentCreatedAt,
        updatedAt: now,
        isArchived: false,
        archived: false,
        archivedAt: null
      },
      { merge: true }
    );
    assignmentsCreated += 1;
  }

  return { quizzesCreated, assignmentsCreated };
}

/**
 * Seed inline materials based on MATERIAL_SEEDS (no filesystem).
 */
async function seedMaterials(db, teacherId, topics) {
  let created = 0;
  let updated = 0;

  if (!Array.isArray(topics) || topics.length === 0) {
    throw new Error('[seed] No topics were created; cannot attach materials.');
  }

  for (const material of MATERIAL_SEEDS) {
    const materialSlug =
      material.id || slugify(material.topicName || material.title || '');
    const topic =
      topics.find((t) => t.name === material.topicName) ||
      topics.find(
        (t) => t.slug && materialSlug && t.slug.includes(materialSlug)
      ) ||
      topics[0];
    const id = `seed-${material.id || slugify(material.title)}`;
    const ref = db.collection('materials').doc(id);
    const existing = await ref.get();
    const createdAt =
      existing.exists && existing.get('createdAt') !== undefined
        ? toMillis(existing.get('createdAt'), Date.now())
        : Date.now();

    await ref.set(
      {
        teacherId,
        classroomId: classroomConfig.id,
        classroomName: classroomConfig.name,
        topicId: topic.id,
        topicName: topic.name,
        title: material.title,
        description: material.description,
        body: material.body,
        createdAt,
        updatedAt: Date.now(),
        isArchived: false,
        archived: false,
        archivedAt: null
      },
      { merge: true }
    );

    if (existing.exists) {
      updated += 1;
    } else {
      created += 1;
    }
  }

  return { created, updated };
}

async function main() {
  ensureProjectAndCredentials();
  const auth = admin.auth();
  const db = admin.firestore();
  db.settings({ ignoreUndefinedProperties: true });

  console.log('[seed] Starting demo seed...');
  const teacher = await ensurePasswordUser(auth, teacherConfig, 'teacher');
  const students = [];
  for (const student of studentConfig) {
    students.push(await ensurePasswordUser(auth, student, 'student'));
  }

  await upsertTeacherProfile(db, teacher);
  await Promise.all(students.map((student) => upsertStudentProfile(db, student)));
  await upsertClassroom(
    db,
    teacher.uid,
    students.map((student) => student.uid)
  );
  const topics = await upsertTopics(db, teacher.uid, buildTopicConfigs());

  const topicQuizResult = await seedTopicQuizzesAndAssignments(
    db,
    teacher.uid,
    topics
  );
  const classQuizResult = await seedClassroomPrePostQuizzes(
    db,
    teacher.uid,
    topics
  );
  const materialResult = await seedMaterials(db, teacher.uid, topics);

  console.log('[seed] Done.');
  console.log(`- Teacher: ${teacher.email} (${teacher.uid})`);
  console.log(
    `- Students (${students.length}): ${students.map((s) => s.email).join(', ')}`
  );
  console.log(
    `- Classroom: ${classroomConfig.name} [${classroomConfig.id}] join code ${classroomConfig.joinCode}`
  );
  console.log(`- Topics: ${topics.length} created/updated`);
  console.log(
    `- Topic quizzes: ${topicQuizResult.quizzesCreated} created (one per LAS topic)`
  );
  console.log(
    `- Topic assignments: ${topicQuizResult.assignmentsCreated} created (attempts ${QUIZ_ATTEMPTS_ALLOWED}, scoring ${QUIZ_SCORING_MODE})`
  );
  console.log(
    `- Classroom-wide quizzes: ${classQuizResult.quizzesCreated} created (pre + post)`
  );
  console.log(
    `- Classroom-wide assignments: ${classQuizResult.assignmentsCreated} created (attempts ${QUIZ_ATTEMPTS_ALLOWED}, scoring ${QUIZ_SCORING_MODE})`
  );
  console.log(
    `- Materials: ${materialResult.created} created, ${materialResult.updated} updated (inline LAS summaries)`
  );
  console.log(
    `- Default password: "${DEFAULT_PASSWORD}"${
      SHOULD_RESET_PASSWORDS ? ' (reset applied)' : ''
    }`
  );
}

if (require.main === module) {
  main().catch((error) => {
    console.error('[seed] Failed', error);
    process.exit(1);
  });
}
